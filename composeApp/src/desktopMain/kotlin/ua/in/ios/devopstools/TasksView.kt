package ua.`in`.ios.devopstools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.gson.JsonObject
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

fun getInstallTypeForTask(task: JsonObject): InstallType? {
    val installType = task.get("install_type")?.asString ?: return null
    return try {
        InstallType.createFromTypeName(installType)
    } catch (e: IllegalArgumentException) {
        logger.e("TasksManager", "Помилка при створенні типу встановлення:", e)
        //println("Помилка при створенні типу встановлення: ${e.message}")
        null
    }
}

/**
 * Component function to display available installation options for the current system
 */
@Composable
fun InstallationOptionsSection(task: JsonObject) {
    val tasksManager = TasksManager.getInstance()
    val options = tasksManager.getAvailableInstallationOptions(task)

    if (options.isEmpty()) {
        Text(
            text = "No installation options available for this system",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Available Installation Options",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        options.forEach { (type, patterns) ->
            Text(
                text = type.replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
            )

            patterns.forEach { pattern ->
                Text(
                    text = "• $pattern",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    isOpen: Boolean,
    task: JsonObject,
    onDismissRequest: () -> Unit,
    onSaveRequest: (JsonObject) -> Unit
) {
    if (isOpen) {
        val editedTask = JsonObject().apply {
            // Copy all fields from the original task
            for (entry in task.entrySet()) {
                add(entry.key, entry.value)
            }
        }

        val taskName = remember { mutableStateOf(TextFieldValue(task.get("name")?.asString ?: "")) }
        val taskDescription = remember { mutableStateOf(TextFieldValue(task.get("description")?.asString ?: "")) }

        var name by remember { mutableStateOf(task.get("name")?.asString ?: "") }
        var description by remember { mutableStateOf(task.get("description")?.asString ?: "") }
        var binaryName by remember { mutableStateOf(task.get("binary_name")?.asString ?: "") }
        var installType by remember { mutableStateOf(task.get("install_type")?.asString ?: "") }
        var versionCmd by remember { mutableStateOf(task.get("version_cmd")?.asString ?: "") }
        var installedAs by remember { mutableStateOf(task.get("installed_as")?.asString ?: "") }
        var enabled by remember { mutableStateOf(task.get("enabled")?.asBoolean ?: false) }
        var installVersion by remember { mutableStateOf(task.get("install_version")?.asString ?: "") }

        // For version checking
        var currentVersion by remember { mutableStateOf("") }
        var isCheckingCurrentVersion by remember { mutableStateOf(false) }
        var availableVersions by remember { mutableStateOf(listOf<String>()) }
        var isLoadingVersions by remember { mutableStateOf(false) }
        var selectedVersion by remember { mutableStateOf("") }

        // For installation file selection
        var availableAssets by remember { mutableStateOf(listOf<String>()) }
        var isLoadingAssets by remember { mutableStateOf(false) }
        var selectedAsset by remember { mutableStateOf("") }
        var filteredAssets by remember { mutableStateOf(listOf<String>()) }
        var assetInstallType by remember { mutableStateOf("") }

        // For installation process
        var isInstalling by remember { mutableStateOf(false) }
        var installationStatus by remember { mutableStateOf("") }
        var installationProgress by remember { mutableStateOf(0f) }
        var showInstallationDialog by remember { mutableStateOf(false) }

        // GitHub-specific fields, if they exist
        var githubUrl by remember { mutableStateOf("") }
        var githubApiUrl by remember { mutableStateOf("") }
        var assetDownloadUrl by remember { mutableStateOf("") }

        if (task.has("github")) {
            val githubObj = task.getAsJsonObject("github")
            githubUrl = githubObj.get("url")?.asString ?: ""
            githubApiUrl = githubObj.get("api_url")?.asString ?: ""

            // Load installation file if already selected
            if (githubObj.has("asset")) {
                selectedAsset = githubObj.get("asset")?.asString ?: ""
                assetInstallType = githubObj.get("asset_type")?.asString ?: ""
            }
        }

        // Create scope for coroutines
        val coroutineScope = rememberCoroutineScope()
        val tasksManager = TasksManager.getInstance()
        val settingsManager = SettingsManager.getInstance()

        // Function to check current version
        fun checkCurrentVersion() {
            isCheckingCurrentVersion = true
            currentVersion = "Checking..."

            coroutineScope.launch {
                val installType = getInstallTypeForTask(task)
                if (installType != null) {
                    currentVersion = installType.getCurrentVersion(task)
                    if (currentVersion.isEmpty()) {
                        currentVersion = "Not installed"
                    }
                } else {
                    currentVersion = "Check error"
                }
                isCheckingCurrentVersion = false
            }
        }

        // Function to filter assets by patterns
        fun filterAssetsByPatterns(assets: List<String>, assetUrlMap: Map<String, String>) {
            val options = tasksManager.getAvailableInstallationOptions(task)
            val filtered = mutableListOf<String>()

            // Score each asset by patterns
            val assetScores = mutableMapOf<String, Pair<Int, String>>() // asset -> (score, installType)

            // Initialize scores
            assets.forEach { assetScores[it] = Pair(0, "") }

            // Check each pattern
            options.forEach { (type, patterns) ->
                patterns.forEach { pattern ->
                    val regexPattern = pattern.replace(".", "\\.").replace("*", ".*")
                    val regex = regexPattern.toRegex(RegexOption.IGNORE_CASE)

                    assets.forEach { asset ->
                        if (regex.matches(asset)) {
                            // Store highest score and install type
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

            // Select only those that scored more than 0
            filtered.addAll(assetScores.filter { it.value.first > 0 }.keys.sorted())

            filteredAssets = filtered

            // Select first item if available
            if (filtered.isNotEmpty() && (selectedAsset.isEmpty() || !filtered.contains(selectedAsset))) {
                selectedAsset = filtered[0]
                // Save install type
                assetInstallType = assetScores[selectedAsset]?.second ?: ""
                // Save download URL
                assetDownloadUrl = assetUrlMap[selectedAsset] ?: ""
            } else if (selectedAsset.isNotEmpty() && assetUrlMap.containsKey(selectedAsset)) {
                assetDownloadUrl = assetUrlMap[selectedAsset] ?: ""
            }
        }

        // Function to load available files for selected version
        fun loadAssetsForVersion(version: String) {
            if (installType != "github" || githubApiUrl.isEmpty()) return

            isLoadingAssets = true
            availableAssets = listOf("Loading...")
            selectedAsset = "" // Reset selected asset when version changes
            assetDownloadUrl = "" // Reset download URL

            coroutineScope.launch {
                try {
                    //val releaseUrl = "$githubApiUrl/releases/tags/$version"
                    //val releaseJson = URL(releaseUrl).readText()
                    val release = tasksManager.getGithubReleaseByTag(githubApiUrl, version)
                    //val release = com.google.gson.JsonParser.parseString(releaseJson).asJsonObject

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

                    availableAssets = assets

                    // Filter assets by patterns for current system
                    filterAssetsByPatterns(assets, assetUrlMap)

                } catch (e: Exception) {
                    availableAssets = listOf("Loading error")
                    filteredAssets = emptyList()
                    logger.e("TasksManager", "Error loading assets:", e)
                    //println("Error loading assets: ${e.message}")
                    e.printStackTrace()
                }

                isLoadingAssets = false
            }
        }

        // Function to load available versions from GitHub
        fun loadAvailableVersions() {
            if (installType != "github") return

            isLoadingVersions = true
            availableVersions = listOf("Loading...")

            coroutineScope.launch {
                try {
                    if (githubApiUrl.isNotEmpty()) {
                        val jsonArray = tasksManager.getGithubReleases(githubApiUrl)

                        val versions = mutableListOf<String>()

                        // Безпечно обробляємо nullable JsonArray
                        if (jsonArray != null) {
                            for (i in 0 until jsonArray.size()) {
                                // Отримуємо JsonObject безпечно
                                val releaseElement = jsonArray.get(i)
                                if (releaseElement != null && releaseElement.isJsonObject) {
                                    val release = releaseElement.asJsonObject

                                    // Безпечно отримуємо tag_name
                                    val tagNameElement = release.get("tag_name")
                                    if (tagNameElement != null && tagNameElement.isJsonPrimitive) {
                                        val tagName = tagNameElement.asString
                                        versions.add(tagName)
                                    }
                                }
                            }
                        }

                        if (versions.isNotEmpty()) {
                            availableVersions = versions
                            selectedVersion = versions[0]
                            loadAssetsForVersion(selectedVersion)
                        } else {
                            // Спробуємо отримати останній реліз, якщо список релізів порожній
                            val latestRelease = tasksManager.getLatestGithubRelease(githubApiUrl)
                            if (latestRelease != null) {
                                val tagNameElement = latestRelease.get("tag_name")
                                if (tagNameElement != null && tagNameElement.isJsonPrimitive) {
                                    val tagName = tagNameElement.asString
                                    availableVersions = listOf(tagName)
                                    selectedVersion = tagName
                                    loadAssetsForVersion(selectedVersion)
                                } else {
                                    availableVersions = listOf("No tag name found")
                                }
                            } else {
                                availableVersions = listOf("No releases found")
                            }
                        }
                    } else {
                        availableVersions = listOf("No API URL specified")
                    }
                } catch (e: Exception) {
                    availableVersions = listOf("Loading error")
                    logger.e("TasksManager", "Error loading versions:", e)
                    //println("Error loading versions: ${e.message}")
                    e.printStackTrace()
                }

                isLoadingVersions = false
            }
        }

        // Helper function to execute shell commands
        suspend fun executeCommand(command: String): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    logger.i("TasksManager", "Executing command: $command")
                    //println("Executing command: $command")
                    val process = Runtime.getRuntime().exec(arrayOf("/bin/sh", "-c", command))
                    val exitCode = process.waitFor()

                    if (exitCode != 0) {
                        val errorStream = process.errorStream.bufferedReader().readText()
                        logger.e("TasksManager", "Command failed with exit code $exitCode: $errorStream")
                        //println("Command failed with exit code $exitCode: $errorStream")
                        installationStatus = "Command failed: $errorStream"
                        false
                    } else {
                        true
                    }
                } catch (e: Exception) {
                    logger.e("TasksManager", "Error executing command:", e)
                    //println("Error executing command: ${e.message}")
                    installationStatus = "Command error: ${e.message}"
                    false
                }
            }
        }

        // Helper function to find a binary file in a directory
        suspend fun findBinary(directory: String, binaryName: String): String {
            return withContext(Dispatchers.IO) {
                try {
                    val dir = File(directory)

                    // Find the binary file recursively
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
                    //println("Error finding binary: ${e.message}")
                    ""
                }
            }
        }

        // Function to install the selected tool
        fun installTool() {
            if (isInstalling || selectedAsset.isEmpty() || assetDownloadUrl.isEmpty()) return

            // Show installation dialog
            showInstallationDialog = true
            isInstalling = true
            installationStatus = "Preparing installation..."
            installationProgress = 0.1f

            // Для налагодження виведемо додаткову інформацію
            logger.i("TasksManager", "Installing: $name")
            logger.i("TasksManager", "Selected asset: $selectedAsset")
            logger.i("TasksManager", "Asset type: $assetInstallType")
            logger.i("TasksManager", "Download URL: $assetDownloadUrl")

            //println("Installing: $name")
            //println("Selected asset: $selectedAsset")
            //println("Asset type: $assetInstallType")
            //println("Download URL: $assetDownloadUrl")

            coroutineScope.launch {
                try {
                    // Get temporary directory
                    val tempDir = settingsManager.getString("settings.temp_path", "/tmp")
                    val toolDir = "$tempDir/${name.replace(" ", "_")}"

                    // Create directory if it doesn't exist
                    installationStatus = "Creating temporary directory..."
                    installationProgress = 0.2f
                    val toolDirFile = File(toolDir)
                    if (!toolDirFile.exists()) {
                        toolDirFile.mkdirs()
                    }

                    // Створюємо шлях для завантаження файлу
                    val destinationFile = "$toolDir/$selectedAsset"

                    // Download the file
                    installationStatus = "Downloading file: $selectedAsset"
                    installationProgress = 0.3f

                    val success = withContext(Dispatchers.IO) {
                        try {
                            val destFile = File(destinationFile)
                            if (!destFile.parentFile.exists()) {
                                destFile.parentFile.mkdirs()
                            }
                            logger.i("TasksManager", "Downloading from: $assetDownloadUrl")
                            logger.i("TasksManager", "Downloading to: $destinationFile")

                            //println("Downloading from: $assetDownloadUrl")
                            //println("Downloading to: $destinationFile")

                            URL(assetDownloadUrl).openStream().use { input ->
                                Files.copy(input, Paths.get(destinationFile), StandardCopyOption.REPLACE_EXISTING)
                            }

                            // Перевіряємо чи файл був успішно завантажений
                            val downloadedFile = File(destinationFile)
                            if (!downloadedFile.exists()) {
                                installationStatus = "Download failed: File does not exist after download"
                                return@withContext false
                            }
                            logger.i("TasksManager", "File downloaded successfully: ${downloadedFile.absolutePath}, size: ${downloadedFile.length()} bytes")
                            //println("File downloaded successfully: ${downloadedFile.absolutePath}, size: ${downloadedFile.length()} bytes")
                            true
                        } catch (e: Exception) {
                            logger.e("TasksManager", "Error downloading file:", e)
                            //println("Error downloading file from '$assetDownloadUrl': ${e.message}")
                            installationStatus = "Download error: ${e.message}"
                            false
                        }
                    }

                    if (!success) {
                        installationProgress = 1.0f
                        isInstalling = false
                        return@launch
                    }

                    // Process the file based on its type
                    installationStatus = "Installing..."
                    installationProgress = 0.7f

                    // Для перевірки файлу перед встановленням
                    val fileToInstall = File(destinationFile)
                    if (!fileToInstall.exists()) {
                        installationStatus = "Installation failed: File not found at $destinationFile"
                        installationProgress = 1.0f
                        isInstalling = false
                        return@launch
                    }

                    val installResult = when (assetInstallType) {
                        "deb_based" -> {
                            // Install DEB package
                            val command = "sudo dpkg -i $destinationFile"
                            executeCommand(command)
                        }
                        "rpm_based" -> {
                            // Install RPM package
                            val command = "sudo rpm -i $destinationFile"
                            executeCommand(command)
                        }
                        "package" -> {
                            // Extract and copy binary from package
                            // Extract the archive
                            if (selectedAsset.endsWith(".tar.gz")) {
                                val extractCommand = "tar -xzf $destinationFile -C $toolDir"
                                val extractResult = executeCommand(extractCommand)

                                if (extractResult) {
                                    // Find the binary and copy it to the installation path
                                    val installPath = settingsManager.getString("settings.install_path", "/usr/bin")
                                    val binaryFile = binaryName.ifEmpty { name }

                                    // Search for the binary in the extracted files
                                    val foundBinary = findBinary(toolDir, binaryFile)

                                    if (foundBinary.isNotEmpty()) {
                                        val copyCommand = "sudo cp $foundBinary $installPath/$binaryFile"
                                        val chmodCommand = "sudo chmod +x $installPath/$binaryFile"

                                        executeCommand(copyCommand) && executeCommand(chmodCommand)
                                    } else {
                                        installationStatus = "Binary not found in the package"
                                        false
                                    }
                                } else {
                                    false
                                }
                            } else {
                                installationStatus = "Unsupported package format"
                                false
                            }
                        }
                        "binary" -> {
                            // Copy binary file directly
                            val installPath = settingsManager.getString("settings.install_path", "/usr/bin")
                            val binaryFile = binaryName.ifEmpty { name }

                            // Make the binary executable
                            val chmodCommand = "chmod +x $destinationFile"
                            val copyCommand = "sudo cp $destinationFile $installPath/$binaryFile"
                            val finalChmodCommand = "sudo chmod +x $installPath/$binaryFile"

                            executeCommand(chmodCommand) && executeCommand(copyCommand) && executeCommand(finalChmodCommand)
                        }
                        else -> {
                            installationStatus = "Unsupported installation type: $assetInstallType"
                            false
                        }
                    }

                    installationProgress = 0.9f

                    if (installResult) {
                        installationStatus = "Installation completed successfully"
                        // Refresh current version after installation
                        delay(1000) // Wait a moment for the installation to settle
                        currentVersion = getInstallTypeForTask(task)?.getCurrentVersion(task) ?: "Unknown"
                    } else {
                        installationStatus = "Installation failed"
                    }

                } catch (e: Exception) {
                    installationStatus = "Installation error: ${e.message}"
                    logger.e("TasksManager", "Installation error", e)
                    //println("Installation error: ${e.message}")
                    e.printStackTrace()
                } finally {
                    installationProgress = 1.0f
                    isInstalling = false
                }
            }
        }

        // Check current version when dialog opens
        LaunchedEffect(Unit) {
            checkCurrentVersion()
            if (installType == "github") {
                loadAvailableVersions()
            }
        }

//        LaunchedEffect(githubUrl) {
//            if (githubUrl.isNotEmpty() && githubUrl.contains("github.com")) {
//                val apiUrl = tasksManager.convertGithubUrlToApiUrl(githubUrl)
//                if (apiUrl.isNotEmpty()) {
//                    githubApiUrl = apiUrl
//                    println("Generated API URL: $apiUrl from GitHub URL: $githubUrl")
//                }
//            }
//        }

        // Installation progress dialog
        if (showInstallationDialog) {
            AlertDialog(
                onDismissRequest = {
                    if (!isInstalling) {
                        showInstallationDialog = false
                    }
                },
                title = { Text("Installing ${name}") },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(installationStatus)
                        LinearProgressIndicator(
                            progress = { installationProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showInstallationDialog = false },
                        enabled = !isInstalling
                    ) {
                        Text("Close")
                    }
                },
                properties = DialogProperties(
                    dismissOnBackPress = !isInstalling,
                    dismissOnClickOutside = !isInstalling
                )
            )
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = "Edit Task") },
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
            text = {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        // Top fields at full width
                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            maxLines = 3
                        )

                        // Main fields in two columns
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // First column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = binaryName,
                                    onValueChange = { binaryName = it },
                                    label = { Text("Binary Name") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = installType,
                                    onValueChange = { installType = it },
                                    label = { Text("Installation Type") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true,
                                    readOnly = true,
                                    enabled = false
                                )

                                OutlinedTextField(
                                    value = versionCmd,
                                    onValueChange = { versionCmd = it },
                                    label = { Text("Version Check Command") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )
                            }

                            // Second column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = installedAs,
                                    onValueChange = { installedAs = it },
                                    label = { Text("Installed As") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                // Current version section with update button
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = currentVersion,
                                        onValueChange = { },
                                        label = { Text("Current Version") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        readOnly = true,
                                        enabled = false
                                    )

                                    IconButton(
                                        onClick = { checkCurrentVersion() },
                                        enabled = !isCheckingCurrentVersion
                                    ) {
                                        if (isCheckingCurrentVersion) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp)
                                            )
                                        } else {
                                            Icon(ICON_REFRESH, contentDescription = "Refresh")
                                        }
                                    }
                                }

                                // Version for installation with dropdown list
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    if (installType == "github" && availableVersions.isNotEmpty()) {
                                        // Versions dropdown
                                        var expanded by remember { mutableStateOf(false) }

                                        ExposedDropdownMenuBox(
                                            expanded = expanded,
                                            onExpandedChange = { expanded = it },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            OutlinedTextField(
                                                value = selectedVersion,
                                                onValueChange = { },
                                                label = { Text("Version to Install") },
                                                readOnly = true,
                                                trailingIcon = {
                                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
                                                },
                                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                                singleLine = true
                                            )

                                            ExposedDropdownMenu(
                                                expanded = expanded,
                                                onDismissRequest = { expanded = false }
                                            ) {
                                                if (isLoadingVersions) {
                                                    DropdownMenuItem(
                                                        text = { Text("Loading...") },
                                                        onClick = { }
                                                    )
                                                } else {
                                                    availableVersions.forEach { version ->
                                                        DropdownMenuItem(
                                                            text = { Text(version) },
                                                            onClick = {
                                                                selectedVersion = version
                                                                installVersion = version
                                                                expanded = false
                                                                // Reset selected asset when version changes
                                                                selectedAsset = ""
                                                                assetDownloadUrl = ""
                                                                // Load assets for selected version
                                                                loadAssetsForVersion(version)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }


                                        // Button to refresh version list
                                        IconButton(
                                            onClick = { loadAvailableVersions() },
                                            enabled = !isLoadingVersions
                                        ) {
                                            if (isLoadingVersions) {
                                                CircularProgressIndicator(
                                                    modifier = Modifier.size(24.dp)
                                                )
                                            } else {
                                                Icon(ICON_REFRESH, contentDescription = "Refresh versions")
                                            }
                                        }
                                    } else {
                                        OutlinedTextField(
                                            value = installVersion,
                                            onValueChange = { installVersion = it },
                                            label = { Text("Version to Install") },
                                            modifier = Modifier.fillMaxWidth(),
                                            singleLine = true
                                        )
                                    }
                                }
                            }
                        }

                        // GitHub fields if installation type is github
                        if (installType == "github") {
                            Text(
                                "GitHub Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = githubUrl,
                                    onValueChange = { githubUrl = it },
                                    label = { Text("Repository URL") },
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                    singleLine = true
                                )

                                OutlinedTextField(
                                    value = githubApiUrl,
                                    onValueChange = { githubApiUrl = it },
                                    label = { Text("GitHub API URL") },
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                    singleLine = true,
                                    readOnly = true
                                )
                            }

                            // Installation file selection section
                            if (filteredAssets.isNotEmpty()) {
                                Text(
                                    "Installation File",
                                    style = MaterialTheme.typography.titleSmall,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                var assetExpanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = assetExpanded,
                                    onExpandedChange = { assetExpanded = it },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    OutlinedTextField(
                                        value = selectedAsset,
                                        onValueChange = { },
                                        label = { Text("Installation File") },
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetExpanded)
                                        },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        singleLine = true
                                    )

                                    ExposedDropdownMenu(
                                        expanded = assetExpanded,
                                        onDismissRequest = { assetExpanded = false }
                                    ) {
                                        if (isLoadingAssets) {
                                            DropdownMenuItem(
                                                text = { Text("Loading...") },
                                                onClick = { }
                                            )
                                        } else {
                                            filteredAssets.forEach { asset ->
                                                DropdownMenuItem(
                                                    text = { Text(asset) },
                                                    onClick = {
                                                        selectedAsset = asset
                                                        assetExpanded = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                if (assetInstallType.isNotEmpty()) {
                                    Text(
                                        "File type: $assetInstallType",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                                    )
                                }

                                // Install button
                                Button(
                                    onClick = { installTool() },
                                    enabled = selectedAsset.isNotEmpty() && !isInstalling && assetDownloadUrl.isNotEmpty(),
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                                ) {
                                    Text("Install")
                                }
                            }

                            // GitHub buttons
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { loadAvailableVersions() },
                                    modifier = Modifier.weight(1f),
                                    enabled = !isLoadingVersions && githubApiUrl.isNotEmpty()
                                ) {
                                    Text("Check Available Versions")
                                }

                                OutlinedButton(
                                    onClick = {
                                        if (githubUrl.isNotEmpty()) {
                                            // Open GitHub repository in browser
                                            try {
                                                val url = java.net.URI(githubUrl).toURL()
                                                java.awt.Desktop.getDesktop().browse(url.toURI())
                                            } catch (e: Exception) {
                                                logger.e("TasksManager", "Error opening URL:", e)
                                                //println("Error opening URL: ${e.message}")
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f),
                                    enabled = githubUrl.isNotEmpty()
                                ) {
                                    Text("Open Repository")
                                }
                            }
                        }

                        // Status (enabled) as checkbox
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = enabled,
                                onCheckedChange = { enabled = it }
                            )
                            Text("Enabled", modifier = Modifier.padding(start = 8.dp))
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider()
                        Spacer(modifier = Modifier.height(16.dp))

                        InstallationOptionsSection(task)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        // Update all fields in the task
                        editedTask.addProperty("name", name)
                        editedTask.addProperty("description", description)
                        editedTask.addProperty("binary_name", binaryName)
                        editedTask.addProperty("install_type", installType)
                        editedTask.addProperty("version_cmd", versionCmd)
                        editedTask.addProperty("installed_as", installedAs)
                        editedTask.addProperty("enabled", enabled)
                        editedTask.addProperty("install_version", if (installType == "github") selectedVersion else installVersion)

                        // Update GitHub fields if installation type is github
                        if (installType == "github") {
                            val githubObj = JsonObject()
                            githubObj.addProperty("url", githubUrl)
                            githubObj.addProperty("api_url", githubApiUrl)

                            // Add selected asset
                            if (selectedAsset.isNotEmpty()) {
                                githubObj.addProperty("asset", selectedAsset)
                                githubObj.addProperty("asset_type", assetInstallType)
                            }

                            editedTask.add("github", githubObj)
                        }

                        onSaveRequest(editedTask)
                        onDismissRequest()
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskAddDialog(
    isOpen: Boolean,
    onDismissRequest: () -> Unit,
    onAddRequest: (JsonObject) -> Unit
) {
    if (isOpen) {
        val settingsManager = SettingsManager.getInstance()
        val tasksManager = TasksManager.getInstance()
        // Create a new task with default values
        val newTask = JsonObject()
        // Form field states
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var binaryName by remember { mutableStateOf("") }
        var installType by remember { mutableStateOf("github") } // github as default
        var versionCmd by remember { mutableStateOf("--version") }
        var installedAs by remember { mutableStateOf("") }
        var enabled by remember { mutableStateOf(true) }
        var installVersion by remember { mutableStateOf("latest") }

        // GitHub-specific fields
        var githubUrl by remember { mutableStateOf("") }
        var githubApiUrl by remember { mutableStateOf("") }

        // Form validation
        var isNameValid by remember { mutableStateOf(true) }
        var isUrlValid by remember { mutableStateOf(true) }

        // Validation states
        var nameErrorText by remember { mutableStateOf("") }
        var urlErrorText by remember { mutableStateOf("") }

        // List of available installation types
        val installTypes = settingsManager.getStringArray("settings.install_types")

        // Coroutines
        val coroutineScope = rememberCoroutineScope()

        // Effect to update API URL when GitHub URL changes
        LaunchedEffect(githubUrl) {
            if (githubUrl.isNotEmpty() && githubUrl.contains("github.com")) {
                val apiUrl = tasksManager.convertGithubUrlToApiUrl(githubUrl)
                if (apiUrl.isNotEmpty()) {
                    githubApiUrl = apiUrl
                    logger.i("TasksManager", "Generated API URL: $apiUrl from GitHub URL: $githubUrl")
                    //println("Generated API URL: $apiUrl from GitHub URL: $githubUrl")
                }
            }
        }

        // Form validation function
        fun validateForm(): Boolean {
            var isValid = true

            // Name validation
            if (name.isBlank()) {
                isNameValid = false
                nameErrorText = "Task name cannot be empty"
                isValid = false
            } else {
                isNameValid = true
                nameErrorText = ""
            }

            // URL validation for GitHub
            if (installType == "github" && githubUrl.isBlank()) {
                isUrlValid = false
                urlErrorText = "Repository URL cannot be empty for GitHub type"
                isValid = false
            } else {
                isUrlValid = true
                urlErrorText = ""
            }

            // Ensure API URL is generated for GitHub
            if (installType == "github" && githubUrl.isNotEmpty() && githubApiUrl.isEmpty()) {
                // Try one more time to generate API URL
                val apiUrl = tasksManager.convertGithubUrlToApiUrl(githubUrl)
                if (apiUrl.isNotEmpty()) {
                    githubApiUrl = apiUrl
                } else {
                    isUrlValid = false
                    urlErrorText = "Invalid GitHub repository URL"
                    isValid = false
                }
            }

            return isValid
        }

        AlertDialog(
            onDismissRequest = onDismissRequest,
            title = { Text(text = "Add New Task") },
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.9f),
            text = {
                Box(
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(8.dp)
                    ) {
                        // Task basic information
                        OutlinedTextField(
                            value = name,
                            onValueChange = {
                                name = it
                                isNameValid = true
                            },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            singleLine = true,
                            isError = !isNameValid,
                            supportingText = {
                                if (!isNameValid) {
                                    Text(nameErrorText)
                                }
                            }
                        )

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            maxLines = 3
                        )

                        // Additional fields
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // First column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = binaryName,
                                    onValueChange = { binaryName = it },
                                    label = { Text("Binary name") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true,
                                    placeholder = { Text("Leave empty to use task name") }
                                )

                                // Installation type dropdown
                                var expandedInstallType by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expandedInstallType,
                                    onExpandedChange = { expandedInstallType = it },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    OutlinedTextField(
                                        value = installType,
                                        onValueChange = { },
                                        label = { Text("Installation type") },
                                        readOnly = true,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedInstallType)
                                        },
                                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                                        singleLine = true
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expandedInstallType,
                                        onDismissRequest = { expandedInstallType = false }
                                    ) {
                                        installTypes.forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type) },
                                                onClick = {
                                                    installType = type
                                                    expandedInstallType = false
                                                }
                                            )
                                        }
                                    }
                                }

                                OutlinedTextField(
                                    value = versionCmd,
                                    onValueChange = { versionCmd = it },
                                    label = { Text("Version check command") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true,
                                    placeholder = { Text("--version") }
                                )
                            }

                            // Second column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                OutlinedTextField(
                                    value = installedAs,
                                    onValueChange = { installedAs = it },
                                    label = { Text("Installed as") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true,
                                    placeholder = { Text("Leave empty to use binary name") }
                                )

                                OutlinedTextField(
                                    value = installVersion,
                                    onValueChange = { installVersion = it },
                                    label = { Text("Version to install") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true,
                                    placeholder = { Text("latest") }
                                )

                                // Checkbox for enabled
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = enabled,
                                        onCheckedChange = { enabled = it }
                                    )
                                    Text("Enabled", modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }

                        // GitHub-specific fields
                        if (installType == "github") {
                            Text(
                                "GitHub Settings",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = githubUrl,
                                    onValueChange = {
                                        githubUrl = it
                                        isUrlValid = true
                                    },
                                    label = { Text("Repository URL") },
                                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                                    singleLine = true,
                                    isError = !isUrlValid,
                                    supportingText = {
                                        if (!isUrlValid) {
                                            Text(urlErrorText)
                                        }
                                    },
                                    placeholder = { Text("https://github.com/owner/repo") }
                                )
                            }

                            OutlinedTextField(
                                value = githubApiUrl,
                                onValueChange = { githubApiUrl = it },
                                label = { Text("GitHub API URL") },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                singleLine = true,
                                readOnly = true,
                                placeholder = { Text("https://api.github.com/repos/owner/repo") }
                            )

                            // Button to open repository
                            OutlinedButton(
                                onClick = {
                                    if (githubUrl.isNotEmpty()) {
                                        // Open GitHub repository in browser
                                        try {
                                            val url = java.net.URI(githubUrl).toURL()
                                            java.awt.Desktop.getDesktop().browse(url.toURI())
                                        } catch (e: Exception) {
                                            logger.e("TasksManager", "Error opening URL:", e)
                                            //println("Error opening URL: ${e.message}")
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                enabled = githubUrl.isNotEmpty()
                            ) {
                                Text("Open Repository")
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (validateForm()) {
                            // Fill the fields for the new task
                            newTask.addProperty("name", name)
                            newTask.addProperty("description", description)
                            newTask.addProperty("binary_name", binaryName)
                            newTask.addProperty("install_type", installType)
                            newTask.addProperty("version_cmd", versionCmd)
                            newTask.addProperty("installed_as", installedAs)
                            newTask.addProperty("enabled", enabled)
                            newTask.addProperty("install_version", installVersion)

                            // GitHub-specific fields
                            if (installType == "github") {
                                val githubObj = JsonObject()
                                githubObj.addProperty("url", githubUrl)
                                githubObj.addProperty("api_url", githubApiUrl)
                                newTask.add("github", githubObj)
                            }

                            onAddRequest(newTask)
                            onDismissRequest()
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        )
    }
}

@Composable
fun TasksTable() {
    val tasksManager = TasksManager.getInstance()
    val tasksArray = tasksManager.getTasksArray()
    var tasks by remember { mutableStateOf(emptyList<JsonObject>()) }
    var showLoadStrategyDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadResult by remember { mutableStateOf<Boolean?>(null) }
    val coroutineScope = rememberCoroutineScope()

    // Завантажуємо завдання при першому відображенні компонента
    LaunchedEffect(tasksArray) {
        tasks = buildList {
            if (tasksArray != null) {
                for (i in 0 until tasksArray.size()) {
                    add(tasksArray.get(i).asJsonObject)
                }
            }
        }
    }

    // Функція оновлення списку завдань після будь-якої операції
    fun refreshTasksList() {
        val updatedTasksArray = tasksManager.getTasksArray()
        tasks = buildList {
            if (updatedTasksArray != null) {
                for (i in 0 until updatedTasksArray.size()) {
                    add(updatedTasksArray.get(i).asJsonObject)
                }
            }
        }
    }

    fun refreshTasks() {
        isLoading = true
        val tasksArray = tasksManager.getTasksArray()

        val tasksList = mutableListOf<JsonObject>()
        if (tasksArray != null) {
            for (i in 0 until tasksArray.size()) {
                val task = tasksArray.get(i).asJsonObject
                tasksList.add(task)
            }
        }

        tasks = tasksList
        isLoading = false
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Заголовок
        Text(
            "Task List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Кнопка оновлення завдань
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { showLoadStrategyDialog = true },
                enabled = !isLoading
            ) {
                Text("Update tasks")
            }
            Button(
                onClick = { showAddDialog = true }
            ) {
                Icon(ICON_ADD, contentDescription = "Add")
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }

            // Показуємо індикатор завантаження
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }

            // Показуємо результат завантаження
            loadResult?.let { success ->
                if (success) {
                    Text(
                        "The tasks has been successfully downloaded",
                        color = MaterialTheme.colorScheme.primary
                    )
                } else {
                    Text(
                        "Error Tasks Downloading",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        // Заголовки таблиці
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Name",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Description",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(2f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Install Type",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // Available version
            Text(
                "Available version",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Status",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                "Actions",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(0.5f),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            //Spacer(modifier = Modifier.width(48.dp))
        }

        // Список завдань
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tasks are missing or loaded...")
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tasks) { task ->
                    TaskRow(
                        task = task,
                        onDeleteClick = {
                            // Видаляємо завдання та оновлюємо список
                            val taskName = task.get("name")?.asString ?: return@TaskRow
                            tasksManager.removeTask(taskName)
                            refreshTasksList()
                        },
                        // Додаємо виклик оновлення при редагуванні
                        onTaskUpdated = {
                            refreshTasksList()
                        }
                    )
                    HorizontalDivider(Modifier, DividerDefaults.Thickness, DividerDefaults.color)
                }

            }
        }
    }

    // Add dialog
    if (showAddDialog) {
        TaskAddDialog(
            isOpen = true,
            onDismissRequest = { showAddDialog = false },
            onAddRequest = { newTask ->
                coroutineScope.launch {
                    tasksManager.addTask(newTask)
                    refreshTasks()
                }
            }
        )
    }

    // Download Strategy Selection dialog
    TaskLoadStrategyDialog(
        isOpen = showLoadStrategyDialog,
        onDismissRequest = { showLoadStrategyDialog = false },
        onStrategySelected = { strategy ->
            // We start downloading with the strategy selected
            isLoading = true
            loadResult = null

            // We use previously created Coroutinescope
            coroutineScope.launch {
                val success = tasksManager.reloadTasks(strategy)
                isLoading = false
                loadResult = success

                if (success) {
                    refreshTasksList()
                }
            }
        }
    )
}

@Composable
fun TaskRow(
    task: JsonObject,
    onDeleteClick: () -> Unit,
    onTaskUpdated: () -> Unit
) {
    val tasksManager = TasksManager.getInstance()
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name
        Text(
            text = task.get("name")?.asString ?: "Невідома назва",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Desc
        Text(
            text = task.get("description")?.asString ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(2f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        // Install Type
        Text(
            text = task.get("install_type")?.asString ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        // Available version
        Text(
            text = task.get("install_version")?.asString ?: "",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )

        // Status
        var enabled by remember { mutableStateOf(task.get("enabled")?.asBoolean ?: false) }

        Checkbox(
            checked = enabled,
            onCheckedChange = { isChecked ->
                enabled = isChecked

                // Оновлюємо значення в об'єкті завдання
                task.addProperty("enabled", isChecked)

                // Зберігаємо зміни
                val taskName = task.get("name")?.asString ?: return@Checkbox
                tasksManager.updateTask(taskName, task)
            },
            modifier = Modifier.weight(0.5f)
        )

        // Buttons
        Row(
            modifier = Modifier.padding(start = 8.dp)
        ) {
            // Кнопка редагування
            IconButton(
                onClick = { showEditDialog = true }
            ) {
                Icon(ICON_EDIT, contentDescription = "Редагувати")
            }

            // Кнопка видалення
            IconButton(
                onClick = { showDeleteConfirmation = true }
            ) {
                Icon(ICON_DELETE, contentDescription = "Видалити")
            }
        }

        val taskName = task.get("name")?.asString ?: "Невідоме завдання"
        ConfirmationDialog(
            isOpen = showDeleteConfirmation,
            onDismissRequest = { showDeleteConfirmation = false },
            onConfirm = onDeleteClick,
            title = "Підтвердження видалення",
            text = "Ви дійсно бажаєте видалити завдання \"$taskName\"?",
            confirmButtonText = "Видалити",
            dismissButtonText = "Скасувати"
        )
        // Діалогове вікно редагування завдання
        if (showEditDialog) {
            TaskEditDialog(
                isOpen = showEditDialog,
                task = task,
                onDismissRequest = { showEditDialog = false },
                onSaveRequest = { updatedTask ->
                    val name = task.get("name")?.asString ?: return@TaskEditDialog
                    val result = tasksManager.updateTask(name, updatedTask)
                    if (result) {
                        // Викликаємо функцію оновлення після успішного збереження
                        onTaskUpdated()
                        showEditDialog = false
                    }
                }
            )
        }
    }
}