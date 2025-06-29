package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
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
    var isLoadingAssets: Boolean = false
)

/**
 * Helper class to manage installation process
 */
class GithubInstaller {
    private val logger = Logger.getInstance()
    private val settingsManager = SettingsManager.getInstance()
    private val tasksManager = TasksManager.getInstance()

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

        assets.forEach { assetScores[it] = Pair(0, "") }

        options.forEach { (type, patterns) ->
            patterns.forEach { pattern ->
                val regexPattern = pattern.replace(".", "\\.").replace("*", ".*")
                val regex = regexPattern.toRegex(RegexOption.IGNORE_CASE)

                assets.forEach { asset ->
                    if (regex.matches(asset)) {
                        val currentScore = assetScores[asset]?.first ?: 0
                        val score = when (type) {
                            "deb_based" -> 400
                            "rpm_based" -> 300
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
                logger.i("loadAssetsForVersion", "Api URL: ${state.githubApiUrl}")
                val assets = mutableListOf<String>()
                val assetUrlMap = mutableMapOf<String, String>() // Map asset name to download URL

                if (release?.has("assets") == true && release.get("assets").isJsonArray) {
                    val assetsArray = release.getAsJsonArray("assets")
                    for (i in 0 until assetsArray.size()) {
                        val asset = assetsArray.get(i).asJsonObject
                        val assetName = asset.get("name").asString
                        assets.add(assetName)

                        // Store download URL for each asset
                        if (asset.has("browser_download_url")) {
                            assetUrlMap[assetName] = asset.get("browser_download_url").asString
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

    suspend fun installGithubTask(
        task: JsonObject,
        onStateChange: (InstallationState) -> Unit
    ) {
        //val state = InstallationState()

        try {
            // Initialize state from task
            state.name = task.get("name")?.asString ?: ""
            state.binaryName = task.get("binary_name")?.asString ?: ""
            state.installType = task.get("install_type")?.asString ?: ""
            state.installVersion = task.get("install_version")?.asString ?: ""

            if (task.has("github")) {
                val githubObj = task.getAsJsonObject("github")
                state.githubUrl = githubObj.get("url")?.asString ?: ""
                state.githubApiUrl = githubObj.get("api_url")?.asString ?: ""

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
                    executeCommandSudo("sudo rpm -i $destinationFile")
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

@Composable
fun InstallButton(task: JsonObject, onInstallComplete: () -> Unit) {
    var isInstalling by remember { mutableStateOf(false) }
    var installationStatus by remember { mutableStateOf("") }
    var installationProgress by remember { mutableStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()
    val installer = remember { GithubInstaller() }

    IconButton(
        onClick = {
            coroutineScope.launch {
                installer.installGithubTask(task) { state ->
                    isInstalling = state.isInstalling
                    installationStatus = state.installationStatus
                    installationProgress = state.installationProgress
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
        }
    }

    // Optional status display
    if (installationStatus.isNotEmpty()) {
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
