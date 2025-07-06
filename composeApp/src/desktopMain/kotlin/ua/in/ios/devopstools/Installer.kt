package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.text.ifEmpty


/**
 * Model class representing the installation process state
 */
data class InstallationState(
    var isInstalling: Boolean = false,
    var selectedAsset: String = "",
    var assetDownloadUrl: String = "",
    var filteredAssets: List<String> = emptyList(),
    var installationStatus: String = "",
    var installationProgress: Float = 0f,
    var name: String = "",
    var assetInstallType: String = "",
    var binaryName: String = "",
    var currentVersion: String = "",
    var githubUrl: String = "",
    var githubApiUrl: String = "",
    var installType: String = "",
    var installVersion: String = "",
    var isLoadingAssets: Boolean = false,
    var parseReleaseNotes: Boolean = false,
    var packageLink: String = "",
    var afterUnpackInstallFlag: Boolean = false,
    var afterUnpackInstallSudo: Boolean = false,
    var afterUnpackInstallCmd: String = ""
)

/**
 * Helper class to manage installation process
 */
class GithubInstaller {
    val state = InstallationState()

    private suspend fun findBinary(directory: String, binaryName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val dir = File(directory)

                fun searchRecursively(dir: File): File? {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name == binaryName && file.canExecute()) {
                            return file
                        } else if (file.isDirectory) {
                            val found = searchRecursively(file)
                            if (found != null) return found
                        }
                    }
                    return null
                }

                val binaryFile = searchRecursively(dir)
                binaryFile?.absolutePath ?: ""
            } catch (e: Exception) {
                logger.e("TasksManager", "Error finding binary file:", e)
                ""
            }
        }
    }

    private fun filterAssetsByPatterns(
        assets: List<String>,
        assetUrlMap: Map<String, String>,
        task: JsonObject,
        state: InstallationState
    ) {
        val options = tasksManager.getAvailableInstallationOptions(task)
        val filtered = mutableListOf<String>()
        val assetScores = mutableMapOf<String, Pair<Int, String>>()
        //val systemInfo = SystemInfo.getInstance()

        assets.forEach { assetScores[it] = Pair(0, "") }

        options.forEach { (type, patterns) ->
            patterns.forEach { pattern ->
                val regexPattern = pattern.replace(".", "\\.").replace("*", ".*")
                val regex = regexPattern.toRegex(RegexOption.IGNORE_CASE)

                assets.forEach { asset ->
                    if (regex.matches(asset)) {
                        val currentScore = assetScores[asset]?.first ?: 0
                        val score = when (type) {
                            "deb_based" -> if (systemInfo.supportsDeb) 400 else 0
                            "rpm_based" -> if (systemInfo.supportsRpm) 300 else 0
                            //"arch_based" -> if (systemInfo.supportsPacman) 400 else 0
                            //"suse_based" -> if (systemInfo.supportsZypper) 300 else 0
                            "package" -> 200
                            "binary" -> 100
                            else -> 50
                        }


                        if (score > currentScore) {
                            assetScores[asset] = Pair(score, type)
                        }
                    }
                }
            }
        }

        filtered.addAll(assetScores.filter { it.value.first > 0 }.keys.sorted())
        state.filteredAssets = filtered

        if (filtered.isNotEmpty() && (state.selectedAsset.isEmpty() || !filtered.contains(state.selectedAsset))) {
            state.selectedAsset = filtered[0]
            state.assetInstallType = assetScores[state.selectedAsset]?.second ?: ""
            state.assetDownloadUrl = assetUrlMap[state.selectedAsset] ?: ""
        } else if (state.selectedAsset.isNotEmpty() && assetUrlMap.containsKey(state.selectedAsset)) {
            state.assetDownloadUrl = assetUrlMap[state.selectedAsset] ?: ""
        }
    }

    // Function to load available files for selected version
    suspend fun loadAssetsForVersion(task: JsonObject, state: InstallationState) {
        return withContext(Dispatchers.IO) {
            try {
                val release = tasksManager.getGithubReleaseByTag(state.githubApiUrl, state.installVersion)
                logger.i("GithubInstaller.loadAssetsForVersion", "Api URL: ${state.githubApiUrl}")
                val assets = mutableListOf<String>()
                val assetUrlMap = mutableMapOf<String, String>() // Map asset name to download URL

//                if (release?.has("assets") == true && release.get("assets").isJsonArray) {
//                    val assetsArray = release.getAsJsonArray("assets")
//                    for (i in 0 until assetsArray.size()) {
//                        val asset = assetsArray.get(i).asJsonObject
//                        val assetName = asset.get("name").asString
//                        assets.add(assetName)
//
//                        // Store download URL for each asset
//                        if (asset.has("browser_download_url")) {
//                            assetUrlMap[assetName] = asset.get("browser_download_url").asString
//                        }
//                    }
//                }

                if (release != null) {
                    if (state.parseReleaseNotes && release.has("body")) {
                        // Парсимо посилання з release notes
                        val body = release.get("body").asString
                        val (parsedAssets, parsedUrls) = parseLinksFromReleaseNotes(body)
                        assets.addAll(parsedAssets)
                        assetUrlMap.putAll(parsedUrls)
                    } else if (release.has("assets") && release.get("assets").isJsonArray) {
                        // Стандартна обробка assets
                        val assetsArray = release.getAsJsonArray("assets")
                        for (i in 0 until assetsArray.size()) {
                            val asset = assetsArray.get(i).asJsonObject
                            val assetName = asset.get("name").asString
                            assets.add(assetName)

                            if (asset.has("browser_download_url")) {
                                assetUrlMap[assetName] = asset.get("browser_download_url").asString
                            }
                        }
                    }
                }

                filterAssetsByPatterns(assets, assetUrlMap, task, state)
            } catch (e: Exception) {
                logger.e("loadAssetsForVersion", "Error loading assets:", e)
                e.printStackTrace()
            }
        }
    }

    suspend fun installTask(
        task: JsonObject,
        onStateChange: (InstallationState) -> Unit
    ) {
        try {
            // Initialize state from task
            state.name = task.get("name")?.asString ?: ""
            state.binaryName = task.get("binary_name")?.asString ?: ""
            state.installType = task.get("install_type")?.asString ?: ""
            state.installVersion = task.get("install_version")?.asString ?: ""

            if (task.has(StaticSettings.InstallTypes.GITHUB)) {
                val githubObj = task.getAsJsonObject(StaticSettings.InstallTypes.GITHUB)
                state.githubUrl = githubObj.get("url")?.asString ?: ""
                state.githubApiUrl = githubObj.get("api_url")?.asString ?: ""
                state.parseReleaseNotes = githubObj.get("parse_release_notes")?.asBoolean ?: false

                if (githubObj.has("asset")) {
                    state.selectedAsset = githubObj.get("asset")?.asString ?: ""
                    state.assetInstallType = githubObj.get("asset_type")?.asString ?: ""
                }
            }

            // Start installation process
            state.isInstalling = true
            state.installationStatus = "Preparing..."
            state.installationProgress = 0.1f
            onStateChange(state)

            // Get temporary directory
            val tempDir = settingsManager.getString("settings.temp_path", "/tmp")
            val toolDir = "$tempDir/${state.name.replace(" ", "_")}"

            // Create directory
            state.installationStatus = "Make temp dir..."
            state.installationProgress = 0.2f
            onStateChange(state)

            val resultDelete = deleteDirectoryRecursivelySync(File(toolDir))
            if (resultDelete) {
                logger.i("TaskEditDialog.githubInstallTool", "Temporary directory deleted $toolDir")
            } else {
                logger.e("TaskEditDialog.githubInstallTool", "Error delete temporary directory $toolDir")
            }

            val toolDirFile = File(toolDir)
            if (!toolDirFile.exists()) {
                toolDirFile.mkdirs()
            }

            loadAssetsForVersion(task,state)

            // Download file
            val destinationFile = "$toolDir/${state.selectedAsset}"
            state.installationStatus = "Downloading file: ${state.selectedAsset}"
            state.installationProgress = 0.3f
            onStateChange(state)

            val downloadSuccess = withContext(Dispatchers.IO) {
                try {
                    val destFile = File(destinationFile)
                    if (!destFile.parentFile.exists()) {
                        destFile.parentFile.mkdirs()
                    }

                    logger.i("installGithubTask", "Downloading from: ${state.assetDownloadUrl}")
                    logger.i("installGithubTask", "Downloading to: $destinationFile")

                    URL(state.assetDownloadUrl).openStream().use { input ->
                        Files.copy(input, Paths.get(destinationFile), StandardCopyOption.REPLACE_EXISTING)
                    }

                    val downloadedFile = File(destinationFile)
                    if (!downloadedFile.exists()) {
                        state.installationStatus = "Download failed: File does not exist after download"
                        onStateChange(state)
                        return@withContext false
                    }

                    logger.i("installGithubTask", "File downloaded successfully: ${downloadedFile.absolutePath}, size: ${downloadedFile.length()} bytes")
                    true
                } catch (e: Exception) {
                    logger.e("installGithubTask", "Error downloading file:", e)
                    state.installationStatus = "Download error: ${e.message}"
                    onStateChange(state)
                    false
                }
            }

            if (!downloadSuccess) {
                state.installationProgress = 1.0f
                state.isInstalling = false
                onStateChange(state)
                return
            }

            // Install process
            state.installationStatus = "Installing..."
            state.installationProgress = 0.7f
            onStateChange(state)

            val fileToInstall = File(destinationFile)
            if (!fileToInstall.exists()) {
                state.installationStatus = "Installation failed: File not found at $destinationFile"
                state.installationProgress = 1.0f
                state.isInstalling = false
                onStateChange(state)
                return
            }

            // Install based on asset type
            val installResult = when (state.assetInstallType) {
                "deb_based" -> {
                    executeCommandSudo("sudo dpkg -i $destinationFile")
                }
                "rpm_based" -> {
                    // Obtain the package name from the RPM file.
                    val queryCommand = "rpm -qp --queryformat '%{NAME}' $destinationFile"
                    val (queryExitCode, packageName) = executeCommandDirect(queryCommand)

                    if (queryExitCode == 0 && packageName.isNotEmpty()) {
                        // Checking if the package is installed.
                        val checkCommand = "rpm -q $packageName"
                        val (checkExitCode, _) = executeCommandDirect(checkCommand)

                        // If the package is installed - remove it.
                        if (checkExitCode == 0) {
                            val removeCommand = "sudo rpm -e $packageName"
                            executeCommandSudo(removeCommand)
                        }
                    }

                    // Installing a new package.
                    val installCommand = "sudo rpm -i $destinationFile"
                    executeCommandSudo(installCommand)
                }
                "package" -> {
                    if (state.selectedAsset.endsWith(".tar.gz")) {
                        val extractResult = executeCommandSudo("tar -xzf $destinationFile -C $toolDir")

                        if (extractResult) {
                            val installPath = settingsManager.getString("settings.install_path", "/usr/bin")
                            val binaryFile = state.binaryName.ifEmpty { state.name }
                            val foundBinary = findBinary(toolDir, binaryFile)

                            if (foundBinary.isNotEmpty()) {
                                executeCommandSudo("sudo cp $foundBinary $installPath/$binaryFile") &&
                                        executeCommandSudo("sudo chmod +x $installPath/$binaryFile")
                            } else {
                                state.installationStatus = "Binary not found in the package"
                                false
                            }
                        } else {
                            false
                        }
                    } else {
                        state.installationStatus = "Unsupported package format"
                        false
                    }
                }
                "binary" -> {
                    val installPath = settingsManager.getString("settings.install_path", "/usr/bin")
                    val binaryFile = state.binaryName.ifEmpty { state.name }
                    executeCommandSudo("chmod +x $destinationFile") &&
                            executeCommandSudo("sudo cp $destinationFile $installPath/$binaryFile") &&
                            executeCommandSudo("sudo chmod +x $installPath/$binaryFile")
                }
                else -> {
                    state.installationStatus = "Unsupported installation type: ${state.assetInstallType}"
                    false
                }
            }

            state.installationProgress = 0.9f
            onStateChange(state)

            if (installResult) {
                state.installationStatus = "Success"
                delay(1000)
                state.currentVersion = getInstallTypeForTask(task)?.getCurrentVersion(task) ?: "Unknown"
            } else {
                state.installationStatus = "Failed"
            }

        } catch (e: Exception) {
            state.installationStatus = "Error: ${e.message}"
            logger.e("installGithubTask", "Installation error", e)
        } finally {
            state.installationProgress = 1.0f
            state.isInstalling = false
            onStateChange(state)
        }
    }
}

class PackageInstaller {
    val state = InstallationState()

    private suspend fun findBinary(directory: String, binaryName: String): String {
        return withContext(Dispatchers.IO) {
            try {
                val dir = File(directory)

                fun searchRecursively(dir: File): File? {
                    dir.listFiles()?.forEach { file ->
                        if (file.isFile && file.name == binaryName && file.canExecute()) {
                            return file
                        } else if (file.isDirectory) {
                            val found = searchRecursively(file)
                            if (found != null) return found
                        }
                    }
                    return null
                }

                val binaryFile = searchRecursively(dir)
                binaryFile?.absolutePath ?: ""
            } catch (e: Exception) {
                logger.e("TasksManager", "Error finding binary file:", e)
                ""
            }
        }
    }

    suspend fun installTask(
        task: JsonObject,
        onStateChange: (InstallationState) -> Unit
    ) {
        try {
            // Initialize state from task
            state.name = task.get("name")?.asString ?: ""
            state.binaryName = task.get("binary_name")?.asString ?: ""
            state.installType = task.get("install_type")?.asString ?: ""
            state.installVersion = task.get("install_version")?.asString ?: ""

            if (task.has(StaticSettings.InstallTypes.PACKAGE)) {
                val packageObj = task.getAsJsonObject(StaticSettings.InstallTypes.PACKAGE)
                state.packageLink = packageObj.get("link")?.asString ?: ""

                if (packageObj.has("after_unpack_install_cmd")) {
                    state.afterUnpackInstallFlag = packageObj.get("flag")?.asBoolean ?: false
                    state.afterUnpackInstallSudo = packageObj.get("sudo")?.asBoolean ?: false
                    state.afterUnpackInstallCmd = packageObj.get("cmd")?.asString ?: ""
                }
            }

            // Start installation process
            state.isInstalling = true
            state.installationStatus = "Preparing..."
            state.installationProgress = 0.1f
            onStateChange(state)

            // Get temporary directory
            val tempDir = settingsManager.getString("settings.temp_path", "/tmp")
            val toolDir = "$tempDir/${state.name.replace(" ", "_")}"
            val downloadFilename = getFilenameFromUrl(state.packageLink)

            // Create directory
            state.installationStatus = "Make temp dir..."
            state.installationProgress = 0.2f
            onStateChange(state)

            val resultDelete = deleteDirectoryRecursivelySync(File(toolDir))
            if (resultDelete) {
                logger.i("PackageInstaller.packageInstallTool", "Temporary directory deleted $toolDir")
            } else {
                logger.e("PackageInstaller.packageInstallTool", "Error delete temporary directory $toolDir")
            }

            val toolDirFile = File(toolDir)
            if (!toolDirFile.exists()) {
                toolDirFile.mkdirs()
            }
            // Download file
            val destinationFile = "$toolDir/${downloadFilename}"
            state.installationStatus = "Downloading file: ${downloadFilename}"
            state.installationProgress = 0.3f
            onStateChange(state)

            val downloadSuccess = withContext(Dispatchers.IO) {
                try {
                    val destFile = File(destinationFile)
                    if (!destFile.parentFile.exists()) {
                        destFile.parentFile.mkdirs()
                    }

                    logger.i("installPackageTask", "Downloading from: ${state.packageLink}")
                    logger.i("installPackageTask", "Downloading to: $destinationFile")

                    URL(state.packageLink).openStream().use { input ->
                        Files.copy(input, Paths.get(destinationFile), StandardCopyOption.REPLACE_EXISTING)
                    }

                    val downloadedFile = File(destinationFile)
                    if (!downloadedFile.exists()) {
                        state.installationStatus = "Download failed: File does not exist after download"
                        onStateChange(state)
                        return@withContext false
                    }

                    logger.i("installPackageTask", "File downloaded successfully: ${downloadedFile.absolutePath}, size: ${downloadedFile.length()} bytes")
                    true
                } catch (e: Exception) {
                    logger.e("installPackageTask", "Error downloading file:", e)
                    state.installationStatus = "Download error: ${e.message}"
                    onStateChange(state)
                    false
                }
            }
            if (!downloadSuccess) {
                state.installationProgress = 1.0f
                state.isInstalling = false
                onStateChange(state)
                return
            }

            // Install process
            state.installationStatus = "Installing..."
            state.installationProgress = 0.7f
            onStateChange(state)

            val fileToInstall = File(destinationFile)
            if (!fileToInstall.exists()) {
                state.installationStatus = "Installation failed: File not found at $destinationFile"
                state.installationProgress = 1.0f
                state.isInstalling = false
                onStateChange(state)
                return
            }
            state.assetInstallType = getFileType(downloadFilename)
            logger.i("TaskEditDialog.packageInstallTool", "File type: $state.assetInstallType")
            val installResult = when (state.assetInstallType) {
                "deb_based" -> {
                    // Install DEB package
                    val command = "sudo dpkg -i $destinationFile"
                    executeCommandSudo(command)
                }
                "rpm_based" -> {
                    // Obtain the package name from the RPM file.
                    val queryCommand = "rpm -qp --queryformat '%{NAME}' $destinationFile"
                    val (queryExitCode, packageName) = executeCommandDirect(queryCommand)

                    if (queryExitCode == 0 && packageName.isNotEmpty()) {
                        // Checking if the package is installed.
                        val checkCommand = "rpm -q $packageName"
                        val (checkExitCode, _) = executeCommandDirect(checkCommand)

                        // If the package is installed - remove it.
                        if (checkExitCode == 0) {
                            val removeCommand = "sudo rpm -e $packageName"
                            executeCommandSudo(removeCommand)
                        }
                    }

                    // Installing a new package.
                    val installCommand = "sudo rpm -i $destinationFile"
                    executeCommandSudo(installCommand)
                }

                "package" -> {
                    var extractResult = false
                    // Extract and copy binary from package
                    // Extract the archive
                    if (downloadFilename.endsWith(".tar.gz") || downloadFilename.endsWith(".tgz")) {
                        val extractCommand = "tar -xzf $destinationFile -C $toolDir"
                        extractResult = executeCommandSudo(extractCommand)
                    } else if (downloadFilename.endsWith(".tar")) {
                        val extractCommand = "tar -xf $destinationFile -C $toolDir"
                        extractResult = executeCommandSudo(extractCommand)
                    } else if (downloadFilename.endsWith(".gz")) {
                        val extractCommand = "gunzip -c $destinationFile > $toolDir/${downloadFilename.removeSuffix(".gz")}"
                        extractResult = executeCommandSudo(extractCommand)
                    } else if (downloadFilename.endsWith(".zip")) {
                        val extractCommand = "unzip $destinationFile -d $toolDir"
                        extractResult = executeCommandSudo(extractCommand)
                    } else if (downloadFilename.endsWith(".xz")) {
                        val extractCommand = "tar -xJf $destinationFile -C $toolDir"
                        extractResult = executeCommandSudo(extractCommand)
                    } else if (downloadFilename.endsWith(".tar.xz")) {
                        val extractCommand = "tar -xJf $destinationFile -C $toolDir"
                        extractResult = executeCommandSudo(extractCommand)
                    } else if (downloadFilename.endsWith(".bz2")) {
                        if (downloadFilename.endsWith(".tar.bz2")) {
                            val extractCommand = "tar -xjf $destinationFile -C $toolDir"
                            extractResult = executeCommandSudo(extractCommand)
                        } else {
                            val extractCommand = "bunzip2 -c $destinationFile > $toolDir/${downloadFilename.removeSuffix(".bz2")}"
                            extractResult = executeCommandSudo(extractCommand)
                        }
                    } else {
                        state.installationStatus = "Unsupported package format"
                        //extractResult = false
                        false
                    }

                    if (extractResult) {
                        // Find the binary and copy it to the installation path
                        if (state.afterUnpackInstallFlag) {
                            val execCmd = "${if (state.afterUnpackInstallSudo) {"sudo "} else {""} }$toolDir/$state.afterUnpackInstallCmd"
                            executeCommandSudo(execCmd)
                        } else {
                            val installPath = settingsManager.getString("settings.install_path", "/usr/bin")
                            val binaryFile = state.binaryName.ifEmpty { state.name }
                            // Search for the binary in the extracted files
                            val foundBinary = findBinary(toolDir, binaryFile)
                            if (foundBinary.isNotEmpty()) {
                                val copyCommand = "sudo cp $foundBinary $installPath/$binaryFile"
                                val chmodCommand = "sudo chmod +x $installPath/$binaryFile"
                                //TODO Temporary comment executing and add true return
                                //true
                                executeCommandSudo(copyCommand) && executeCommandSudo(chmodCommand)
                            } else {
                                state.installationStatus = "Binary not found in the package"
                                false
                            }
                        }
                    } else {
                        false
                    }

                }
                "binary" -> {
                    // Copy binary file directly
                    val installPath = settingsManager.getString("settings.install_path", "/usr/bin")
                    val binaryFile = state.binaryName.ifEmpty { state.name }
                    // Make the binary executable
                    val chmodCommand = "chmod +x $destinationFile"
                    val copyCommand = "sudo cp $destinationFile $installPath/$binaryFile"
                    val finalChmodCommand = "sudo chmod +x $installPath/$binaryFile"
                    executeCommandSudo(chmodCommand) && executeCommandSudo(copyCommand) && executeCommandSudo(finalChmodCommand)
                }
                else -> {
                    state.installationStatus = "Unsupported installation type: $state.assetInstallType"
                    false
                }
            }

            state.installationProgress = 0.9f
            onStateChange(state)

            if (installResult) {
                state.installationStatus = "Success"
                delay(1000)
                state.currentVersion = getInstallTypeForTask(task)?.getCurrentVersion(task) ?: "Unknown"
            } else {
                state.installationStatus = "Failed"
            }

        } catch (e: Exception) {
            state.installationStatus = "Error: ${e.message}"
            logger.e("installPackageTask", "Installation error", e)
        } finally {
            state.installationProgress = 1.0f
            state.isInstalling = false
            onStateChange(state)
    }
    }
}

@Composable
fun InstallButton(task: JsonObject, onInstallComplete: () -> Unit) {
    var isInstalling by remember { mutableStateOf(false) }
    var installationStatus by remember { mutableStateOf("") }
    var installationProgress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val installType = task.get("install_type")?.asString ?: ""
    val installer = remember {
        when (installType) {
            StaticSettings.InstallTypes.GITHUB -> { GithubInstaller() }
            StaticSettings.InstallTypes.PACKAGE -> { PackageInstaller() }
            else -> { null }
        }
    }

    Button(
        modifier =  Modifier.size(width = 130.dp, height = 40.dp),
        colors = ButtonColors(
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        shape = RoundedCornerShape(4.dp),
        onClick = {
            coroutineScope.launch {
                when (installer) {
                    is GithubInstaller -> {
                        installer.installTask(task) { state ->
                            isInstalling = state.isInstalling
                            installationStatus = state.installationStatus
                            installationProgress = state.installationProgress
                        }
                    }
                    is PackageInstaller -> {
                        installer.installTask(task) { state ->
                            isInstalling = state.isInstalling
                            installationStatus = state.installationStatus
                            installationProgress = state.installationProgress
                        }
                    }
                }
                onInstallComplete()
            }
        },
        enabled = !isInstalling

    ) {
        if (isInstalling) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp)
            )
        } else {
            Icon(ICON_PLAY, contentDescription = "Install")
            Spacer(Modifier.width(4.dp))
            //if (installationStatus.isEmpty()) {
                Text("Install")
            //}

        }
    }

    // Optional status display
    if (installationStatus.isNotEmpty()) {
        Spacer(Modifier.width(8.dp))
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(installationStatus)
            LinearProgressIndicator(
                progress = { installationProgress },
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
        }
    }
}
