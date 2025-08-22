package ua.`in`.ios.devopstools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.zIndex
import java.awt.Desktop
import java.net.URI
import kotlin.collections.component1
import kotlin.collections.component2

fun getInstallTypeForTask(task: JsonObject): InstallType? {
    val installType = task.get("install_type")?.asString ?: return null
    return try {
        InstallType.createFromTypeName(installType)
    } catch (e: IllegalArgumentException) {
        logger.e("TasksManager", "Error creating installation type:", e)
        null
    }
}

/**
 * Component function to display available installation options for the current system
 */
@Composable
fun InstallationPatternsSection(task: JsonObject) {
    val options = tasksManager.getAvailableInstallationOptions(task)

    if (options.isEmpty()) {
        Text(
            text = "No installation patterns available for this system",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error
        )
        return
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = "Used Installation patterns",
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
                    text = pattern,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(start = 16.dp, bottom = 2.dp)
                )
            }
        }
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
        // Create a new task with default values
        val newTask = JsonObject()
        // Form field states
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var binaryName by remember { mutableStateOf("") }
        var installType by remember { mutableStateOf(StaticSettings.InstallTypes.GITHUB) } // github as default
        var versionCmd by remember { mutableStateOf("--version") }
        //var installedAs by remember { mutableStateOf("") }
        var enabled by remember { mutableStateOf(true) }
        var installVersion by remember { mutableStateOf("latest") }
        var disableVersionCheck by remember { mutableStateOf(false) }

        // GitHub-specific fields
        var githubUrl by remember { mutableStateOf("") }
        var githubApiUrl by remember { mutableStateOf("") }
        var packageLink by remember { mutableStateOf("") }

        // Form validation
        var isNameValid by remember { mutableStateOf(true) }
        var isUrlValid by remember { mutableStateOf(true) }

        // Validation states
        var nameErrorText by remember { mutableStateOf("") }
        var urlErrorText by remember { mutableStateOf("") }

        // List of available installation types
        val installTypes = StaticSettings.InstallTypes.ENABLED

        // Coroutines
        //val coroutineScope = rememberCoroutineScope()

        // Effect to update API URL when GitHub URL changes
        LaunchedEffect(githubUrl) {
            if (githubUrl.isNotEmpty() && githubUrl.contains("github.com")) {
                val apiUrl = tasksManager.convertGithubUrlToApiUrl(githubUrl)
                if (apiUrl.isNotEmpty()) {
                    githubApiUrl = apiUrl
                    logger.i("TaskAddDialog", "Generated API URL: $apiUrl from GitHub URL: $githubUrl")
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
            if (installType == StaticSettings.InstallTypes.GITHUB && githubUrl.isBlank()) {
                isUrlValid = false
                urlErrorText = "Repository URL cannot be empty for GitHub type"
                isValid = false
            } else {
                isUrlValid = true
                urlErrorText = ""
            }

            // Ensure API URL is generated for GitHub
            if (installType == StaticSettings.InstallTypes.GITHUB && githubUrl.isNotEmpty() && githubApiUrl.isEmpty()) {
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
                                        modifier = Modifier.menuAnchor(
                                            type = MenuAnchorType.PrimaryNotEditable,
                                            enabled = true
                                        ).fillMaxWidth(),
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = disableVersionCheck,
                                        onCheckedChange = { disableVersionCheck = it }
                                    )
                                    Text("Disable version check", modifier = Modifier.padding(start = 8.dp))
                                }
                            }

                            // Second column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
//                                OutlinedTextField(
//                                    value = installedAs,
//                                    onValueChange = { installedAs = it },
//                                    label = { Text("Installed as") },
//                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
//                                    singleLine = true,
//                                    placeholder = { Text("Leave empty to use binary name") }
//                                )

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
                        if (installType == StaticSettings.InstallTypes.GITHUB) {
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
                                    modifier = Modifier.weight(0.5f).padding(vertical = 4.dp),
                                    singleLine = true,
                                    isError = !isUrlValid,
                                    supportingText = {
                                        if (!isUrlValid) {
                                            Text(urlErrorText)
                                        }
                                    },
                                    placeholder = { Text("https://github.com/owner/repo") }
                                )
                                IconButton(
                                    onClick = {
                                        if (githubUrl.isNotEmpty()) {
                                            // Open GitHub repository in browser
                                            try {
                                                val url = URI(githubUrl).toURL()
                                                Desktop.getDesktop().browse(url.toURI())
                                            } catch (e: Exception) {
                                                logger.e("TasksEditDialog", "Error opening URL:", e)
                                            }
                                        }
                                    },
                                    enabled = githubUrl.isNotEmpty()
                                ) {
                                    Icon(ICON_LINK, contentDescription = "Open")
                                    Spacer(Modifier.width(4.dp))
                                }
                                OutlinedTextField(
                                    value = githubApiUrl,
                                    onValueChange = { githubApiUrl = it },
                                    label = { Text("GitHub API URL") },
                                    modifier = Modifier.fillMaxWidth(0.5f).padding(vertical = 4.dp),
                                    singleLine = true,
                                    readOnly = true,
                                    placeholder = { Text("https://api.github.com/repos/owner/repo") }
                                )
                            }



                            // Button to open repository
//                            OutlinedButton(
//                                onClick = {
//                                    if (githubUrl.isNotEmpty()) {
//                                        // Open GitHub repository in browser
//                                        try {
//                                            val url = URI(githubUrl).toURL()
//                                            Desktop.getDesktop().browse(url.toURI())
//                                        } catch (e: Exception) {
//                                            logger.e("TaskAddDialog", "Error opening URL:", e)
//                                        }
//                                    }
//                                },
//                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
//                                enabled = githubUrl.isNotEmpty()
//                            ) {
//                                Text("Open Repository")
//                            }
                        }
                        if (installType == StaticSettings.InstallTypes.PACKAGE) {
                            Text(
                                "Package Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            OutlinedTextField(
                                value = packageLink,
                                onValueChange = { packageLink = it },
                                label = { Text("Package Link") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
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
                            if (binaryName.isEmpty()) {
                                newTask.addProperty("binary_name", name)
                            } else {
                                newTask.addProperty("binary_name", binaryName)
                            }
                            newTask.addProperty("install_type", installType)
                            newTask.addProperty("version_cmd", versionCmd)
                            newTask.addProperty("disable_version_check", disableVersionCheck)
                            //newTask.addProperty("installed_as", installedAs)
                            newTask.addProperty("enabled", enabled)
                            newTask.addProperty("install_version", installVersion)

                            // GitHub-specific fields
                            if (installType == StaticSettings.InstallTypes.GITHUB) {
                                val githubObj = JsonObject()
                                githubObj.addProperty("url", githubUrl)
                                githubObj.addProperty("api_url", githubApiUrl)
                                newTask.add(StaticSettings.InstallTypes.GITHUB, githubObj)
                            }
                            if (installType == StaticSettings.InstallTypes.PACKAGE) {
                                val packageObj = JsonObject()
                                packageObj.addProperty("link", packageLink)
                                newTask.add(StaticSettings.InstallTypes.PACKAGE, packageObj)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditDialog(
    isOpen: Boolean,
    task: JsonObject,
    onDismissRequest: () -> Unit,
    onSaveRequest: (JsonObject) -> Unit
) {
    // Create scope for coroutines
    val coroutineScope = rememberCoroutineScope()
    var showLoadingOverlay by remember { mutableStateOf(false) }
    var isLoadingVersions by remember { mutableStateOf(false) }
    var availableVersions by remember { mutableStateOf(listOf<String>()) }

    if (isOpen) {
        val editedTask = JsonObject().apply {
            // Copy all fields from the original task
            for (entry in task.entrySet()) {
                add(entry.key, entry.value)
            }
        }
        var name by remember { mutableStateOf(task.get("name")?.asString ?: "") }
        var description by remember { mutableStateOf(task.get("description")?.asString ?: "") }
        var binaryName by remember { mutableStateOf(task.get("binary_name")?.asString ?: "") }
        var installType by remember { mutableStateOf(task.get("install_type")?.asString ?: "") }
        var versionCmd by remember { mutableStateOf(task.get("version_cmd")?.asString ?: "") }
        var disableVersionCheck by remember { mutableStateOf(task.get("disable_version_check")?.asBoolean ?: false) }
        //var installedAs by remember { mutableStateOf(task.get("installed_as")?.asString ?: "") }
        var enabled by remember { mutableStateOf(task.get("enabled")?.asBoolean ?: false) }
        var installVersion by remember { mutableStateOf(task.get("install_version")?.asString ?: "") }
        // For version checking
        var currentVersion by remember { mutableStateOf("") }
        var isCheckingCurrentVersion by remember { mutableStateOf(false) }
        //var availableVersions by remember { mutableStateOf(listOf<String>()) }

        //var selectedVersion by remember { mutableStateOf("") }
        var selectedVersion by remember { mutableStateOf(task.get("install_version")?.asString ?: "") }
        // For installation file selection
        //var availableAssets by remember { mutableStateOf(listOf<String>()) }
        var isLoadingAssets by remember { mutableStateOf(false) }
        var selectedAsset by remember { mutableStateOf("") }
        var filteredAssets by remember { mutableStateOf(listOf<String>()) }
        var assetInstallType by remember { mutableStateOf("") }
        var savedAssetType by remember { mutableStateOf("") }
        // For installation process
        var isInstalling by remember { mutableStateOf(false) }
        var installationStatus by remember { mutableStateOf("") }
        var installationProgress by remember { mutableStateOf(0f) }
        var showInstallationDialog by remember { mutableStateOf(false) }
        // GitHub-specific fields, if they exist
        var githubUrl by remember { mutableStateOf("") }
        var githubApiUrl by remember { mutableStateOf("") }
        var assetDownloadUrl by remember { mutableStateOf("") }
        var parseReleaseNotes by remember { mutableStateOf(false) }
        // Package fields
        var packageLink by remember { mutableStateOf("") }
        var afterUnpackInstallFlag by remember { mutableStateOf(false) }
        var afterUnpackInstallCmd by remember { mutableStateOf("") }
        var afterUnpackInstallSudo by remember { mutableStateOf(false) }

        var afterInstallFlag by remember { mutableStateOf(false) }
        var afterInstallCmd by remember { mutableStateOf("") }
        var afterInstallSudo by remember { mutableStateOf(false) }

        if (task.has(StaticSettings.InstallTypes.GITHUB)) {
            val githubObj = task.getAsJsonObject(StaticSettings.InstallTypes.GITHUB)
            githubUrl = githubObj.get("url")?.asString ?: ""
            githubApiUrl = githubObj.get("api_url")?.asString ?: ""
            savedAssetType = githubObj.get("asset_type")?.asString ?: ""
            parseReleaseNotes = githubObj.get("parse_release_notes")?.asBoolean ?: false
            val afterInstallObj = githubObj.getAsJsonObject("after_install_cmd") ?: JsonObject()
            afterInstallFlag = afterInstallObj.get("flag")?.asBoolean ?: false
            afterInstallCmd = afterInstallObj.get("cmd")?.asString ?: ""
            afterInstallSudo = afterInstallObj.get("sudo")?.asBoolean ?: false
        }

        if (task.has(StaticSettings.InstallTypes.PACKAGE)) {
            val packageObj = task.getAsJsonObject(StaticSettings.InstallTypes.PACKAGE)
            packageLink = packageObj.get("link")?.asString ?: ""
            val afterunpackObj = packageObj.getAsJsonObject("after_unpack_install_cmd") ?: JsonObject()
            afterUnpackInstallFlag = afterunpackObj.get("flag")?.asBoolean ?: false
            afterUnpackInstallCmd = afterunpackObj.get("cmd")?.asString ?: ""
            afterUnpackInstallSudo = afterunpackObj.get("sudo")?.asBoolean ?: false
            val afterInstallObj = packageObj.getAsJsonObject("after_install_cmd") ?: JsonObject()
            afterInstallFlag = afterInstallObj.get("flag")?.asBoolean ?: false
            afterInstallCmd = afterInstallObj.get("cmd")?.asString ?: ""
            afterInstallSudo = afterInstallObj.get("sudo")?.asBoolean ?: false
        }

        // Function to check current version
        fun checkCurrentVersion() {
            isCheckingCurrentVersion = true
            currentVersion = "Checking..."

            coroutineScope.launch {
                currentVersion = TaskUtils.checkCurrentVersion(task)
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
                                "deb_based" -> if (systemInfo.supportsDeb) 400 else 0
                                "rpm_based" -> if (systemInfo.supportsRpm) 300 else 0
                                //"arch_based" -> if (systemInfo.supportsPacman) 400 else 0
                                //"suse_based" -> if (systemInfo.supportsZypper) 300 else 0
                                "binary" -> if (type == savedAssetType) 1000 else 200
                                "package" -> if (type == savedAssetType) 1000 else 100
                                else -> 50
                            }
                            if (score > currentScore) {
                                assetScores[asset] = Pair(score, type)
                            }
                        }
                        logger.d("TaskAddDialog.filterAssetsByPatterns", "Regex: $regex, asset: $asset, score: ${assetScores[asset]?.first}")
                    }
                }
            }

            // Select only those that scored more than 0
            logger.d("TaskAddDialog.filterAssetsByPatterns", "assetScores before filter: $assetScores")
            filtered.addAll(assetScores.filter { it.value.first > 0 }.keys.sorted())
            logger.d("TaskAddDialog.filterAssetsByPatterns", "assetScores after filter: $assetScores")
            filteredAssets = filtered
            logger.d("TaskAddDialog.filterAssetsByPatterns", "filteredAssets: $filteredAssets")

            // Select first item if available
            if (filtered.isNotEmpty() && (selectedAsset.isEmpty() || !filtered.contains(selectedAsset))) {
                selectedAsset = assetScores.entries
                    .filter { filtered.contains(it.key) }
                    .maxByOrNull { it.value.first }
                    ?.key ?: filtered[0]
                logger.d("TaskAddDialog.filterAssetsByPatterns", "selectedAsset: $selectedAsset")
                // Save install type
                assetInstallType = assetScores[selectedAsset]?.second ?: ""
                // Save download URL
                assetDownloadUrl = assetUrlMap[selectedAsset] ?: ""
            } else if (selectedAsset.isNotEmpty() && assetUrlMap.containsKey(selectedAsset)) {
                assetDownloadUrl = assetUrlMap[selectedAsset] ?: ""
            }
            logger.d("TaskAddDialog.filterAssetsByPatterns", "Selected asset: $selectedAsset, install type: $assetInstallType, download URL: $assetDownloadUrl")
        }

        // Function to load available files for a selected version
        fun githubLoadAssetsForVersion(version: String) {
            if (installType != StaticSettings.InstallTypes.GITHUB || githubApiUrl.isEmpty()) return

            isLoadingAssets = true
            selectedAsset = "" // Reset selected asset when version changes
            assetDownloadUrl = "" // Reset download URL

            coroutineScope.launch {
                try {
                    val release = tasksManager.getGithubReleaseByTag(githubApiUrl, version)
                    val assets = mutableListOf<String>()
                    val assetUrlMap = mutableMapOf<String, String>()

                    if (release != null) {
                        if (parseReleaseNotes && release.has("body")) {
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

                    //availableAssets = assets
                    // Filter assets by patterns for current system
                    filterAssetsByPatterns(assets, assetUrlMap)

                } catch (e: Exception) {
                    //availableAssets = listOf("Loading error")
                    filteredAssets = emptyList()
                    logger.e("TasksManager", "Error loading assets:", e)
                    e.printStackTrace()
                }

                isLoadingAssets = false
            }
        }

        // Function to load available versions from GitHub
        fun githubLoadAvailableVersions() {
            if (installType != StaticSettings.InstallTypes.GITHUB) return
            val CurrentSelectedVersion = selectedVersion
            coroutineScope.launch(Dispatchers.Main) {
                showLoadingOverlay = true
                isLoadingVersions = true
                availableVersions = listOf("Loading...")
                logger.d("LoadVersions", "Setting showLoadingOverlay to ${showLoadingOverlay}")
            }

            coroutineScope.launch(Dispatchers.IO) {
                try {
                    if (githubApiUrl.isNotEmpty()) {
                        val jsonArray = tasksManager.getGithubReleases(githubApiUrl)
                        val versions = mutableListOf<String>()

                        if (jsonArray != null) {
                            for (i in 0 until jsonArray.size()) {
                                // Отримуємо JsonObject безпечно
                                val releaseElement = jsonArray.get(i)
                                if (releaseElement != null && releaseElement.isJsonObject) {
                                    val release = releaseElement.asJsonObject
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

                            selectedVersion = if (selectedVersion.isNotEmpty()) {
                                CurrentSelectedVersion
                            } else {
                                versions[0]
                            }
                            githubLoadAssetsForVersion(selectedVersion)

                        } else {
                            // TODO
                            val latestRelease = tasksManager.getLatestGithubRelease(githubApiUrl)
                            if (latestRelease != null) {
                                val tagNameElement = latestRelease.get("tag_name")
                                if (tagNameElement != null && tagNameElement.isJsonPrimitive) {
                                    val tagName = tagNameElement.asString
                                    availableVersions = listOf(tagName)
                                    selectedVersion = tagName
                                    githubLoadAssetsForVersion(selectedVersion)
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
                    withContext(Dispatchers.Main) {
                        availableVersions = listOf("Loading error")
                    }
                    logger.e("TasksManager", "Error loading versions:", e)
                    e.printStackTrace()
                } finally {
                    withContext(Dispatchers.Main) {
                        isLoadingVersions = false
                        showLoadingOverlay = false
                    }
                }
            }
        }

        fun makeEditedTask(): JsonObject {
            editedTask.addProperty("name", name)
            editedTask.addProperty("description", description)
            editedTask.addProperty("binary_name", binaryName)
            editedTask.addProperty("install_type", installType)
            editedTask.addProperty("version_cmd", versionCmd)
            editedTask.addProperty("disable_version_check", disableVersionCheck)
            editedTask.addProperty("enabled", enabled)
            editedTask.addProperty("install_version", if (installType == StaticSettings.InstallTypes.GITHUB) selectedVersion else installVersion)

            // Update GitHub fields if installation type is github
            if (installType == StaticSettings.InstallTypes.GITHUB) {
                val githubObj = JsonObject()
                githubObj.addProperty("url", githubUrl)
                githubObj.addProperty("api_url", githubApiUrl)
                githubObj.addProperty("parse_release_notes", parseReleaseNotes)

                // Add selected asset
                if (selectedAsset.isNotEmpty()) {
                    githubObj.addProperty("asset", selectedAsset)
                    githubObj.addProperty("asset_type", assetInstallType)
                }

                val afterInstallObj = JsonObject().apply {
                    addProperty("flag", afterInstallFlag)
                    addProperty("sudo", afterInstallSudo)
                    addProperty("cmd", afterInstallCmd)
                }
                githubObj.add("after_install_cmd", afterInstallObj)

                editedTask.add(StaticSettings.InstallTypes.GITHUB, githubObj)
            }
            if (installType == StaticSettings.InstallTypes.PACKAGE) {
                val packageObj = JsonObject()
                packageObj.addProperty("link", packageLink)
                val afterunpackObj = JsonObject().apply {
                    addProperty("flag", afterUnpackInstallFlag)
                    addProperty("sudo", afterUnpackInstallSudo)
                    addProperty("cmd", afterUnpackInstallCmd)
                }
                packageObj.add("after_unpack_install_cmd", afterunpackObj)

                val afterInstallObj = JsonObject().apply {
                    addProperty("flag", afterInstallFlag)
                    addProperty("sudo", afterInstallSudo)
                    addProperty("cmd", afterInstallCmd)
                }
                packageObj.add("after_install_cmd", afterInstallObj)

                editedTask.add(StaticSettings.InstallTypes.PACKAGE, packageObj)
            }
            return editedTask
        }

        // Check current version when dialog opens
        LaunchedEffect(Unit) {
            checkCurrentVersion()
            if (installType == StaticSettings.InstallTypes.GITHUB) {
                githubLoadAvailableVersions()
            }
        }
        LaunchedEffect(selectedAsset) {
            assetInstallType = getFileType(selectedAsset)
        }

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
                if (showLoadingOverlay) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .zIndex(Float.POSITIVE_INFINITY)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0x80000000)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading versions...",
                                color = Color.White,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
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
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                                ) {
                                    Checkbox(
                                        checked = disableVersionCheck,
                                        onCheckedChange = { disableVersionCheck = it }
                                    )
                                    Text("Disable version check", modifier = Modifier.padding(start = 8.dp))
                                }
                            }

                            // Second column
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
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
                                    if (installType == StaticSettings.InstallTypes.GITHUB && availableVersions.isNotEmpty()) {
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
                                                modifier = Modifier.menuAnchor(
                                                    type = MenuAnchorType.PrimaryNotEditable,
                                                    enabled = true
                                                ).fillMaxWidth(),
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
                                                                githubLoadAssetsForVersion(version)
                                                            }
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        // Button to refresh version list
                                        IconButton(
                                            onClick = { githubLoadAvailableVersions() },
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
                        if (installType == StaticSettings.InstallTypes.GITHUB) {
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
                                    modifier = Modifier.weight(0.5f).padding(vertical = 4.dp),
                                    singleLine = true
                                )
                                IconButton(
                                    onClick = {
                                        if (githubUrl.isNotEmpty()) {
                                            // Open GitHub repository in browser
                                            try {
                                                val url = URI(githubUrl).toURL()
                                                Desktop.getDesktop().browse(url.toURI())
                                            } catch (e: Exception) {
                                                logger.e("TasksEditDialog", "Error opening URL:", e)
                                            }
                                        }
                                    },
                                    enabled = githubUrl.isNotEmpty()
                                ) {
                                    Icon(ICON_LINK, contentDescription = "Open")
                                    Spacer(Modifier.width(4.dp))
                                }
                                OutlinedTextField(
                                    value = githubApiUrl,
                                    onValueChange = { githubApiUrl = it },
                                    label = { Text("GitHub API URL") },
                                    modifier = Modifier.weight(0.5f).padding(vertical = 4.dp),
                                    singleLine = true,
                                    readOnly = true
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = parseReleaseNotes,
                                    onCheckedChange = { parseReleaseNotes = it }
                                )
                                Text("Parse Release Notes for download links", modifier = Modifier.padding(start = 8.dp))
                            }
                            // Installation file selection section
                                if (filteredAssets.isNotEmpty()) {
                                    Text(
                                        "Installation File",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                    var assetExpanded by remember { mutableStateOf(false) }
                                    // Asset to install
                                    ExposedDropdownMenuBox(
                                        expanded = assetExpanded,
                                        onExpandedChange = { assetExpanded = it },
                                    ) {
                                        OutlinedTextField(
                                            value = selectedAsset,
                                            onValueChange = { },
                                            label = { Text("Installation File") },
                                            readOnly = true,
                                            trailingIcon = {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetExpanded)
                                            },
                                            modifier = Modifier.menuAnchor(
                                                type = MenuAnchorType.PrimaryNotEditable,
                                                enabled = true
                                            ).fillMaxWidth(),

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
                                    if (selectedAsset.isNotEmpty() && assetDownloadUrl.isNotEmpty() && enabled) {
                                        makeEditedTask()
                                        InstallButton(
                                            editedTask,
                                            onInstallComplete = {
                                                checkCurrentVersion()
                                            /*refreshTasks()*/
                                            }
                                        )
                                    }
                                }
                        }

                        if (installType == StaticSettings.InstallTypes.PACKAGE) {
                            Text(
                                "Package Parameters",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                            OutlinedTextField(
                                value = packageLink,
                                onValueChange = { packageLink = it },
                                label = { Text("Package Link") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            // After Unpack
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = afterUnpackInstallFlag,
                                    onCheckedChange = { afterUnpackInstallFlag = it }
                                )
                                Text("Execute a command after unpack ", modifier = Modifier.padding(start = 8.dp))
                            }
                                if (afterUnpackInstallFlag) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                    Checkbox(
                                        checked = afterUnpackInstallSudo,
                                        onCheckedChange = { afterUnpackInstallSudo = it }
                                    )
                                    Text("Execute with sudo", modifier = Modifier.padding(start = 8.dp))
                                    Spacer(Modifier.width(16.dp))
                                    OutlinedTextField(
                                        value = afterUnpackInstallCmd,
                                        onValueChange = { afterUnpackInstallCmd = it },
                                        label = { Text("After unpack command") },
                                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                        singleLine = true,
                                        readOnly = false,
                                        enabled = true
                                    )
                                }
                            }
                            // End After unpack

                            if (packageLink.isNotEmpty() && enabled) {
                                makeEditedTask()
                                InstallButton(
                                    editedTask,
                                    onInstallComplete = {
                                        checkCurrentVersion()
                                        /*refreshTasks()*/
                                    }
                                )
                            }
                        }


                        // After Install
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = afterInstallFlag,
                                onCheckedChange = { afterInstallFlag = it }
                            )
                            Text("Execute a command after install ", modifier = Modifier.padding(start = 8.dp))
                        }
                        if (afterInstallFlag) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = afterInstallSudo,
                                    onCheckedChange = { afterInstallSudo = it }
                                )
                                Text("Execute with sudo", modifier = Modifier.padding(start = 8.dp))
                                Spacer(Modifier.width(16.dp))
                                OutlinedTextField(
                                    value = afterInstallCmd,
                                    onValueChange = { afterInstallCmd = it },
                                    label = { Text("After install command") },
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    singleLine = true,
                                    readOnly = false,
                                    enabled = true
                                )
                            }
                        }
                        // End After install

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
                        if (settingsManager.getString("settings.log_level") == "DEBUG" || settingsManager.getString("settings.log_level") == "DEV") {
                            InstallationPatternsSection(task)
                        }

                    }
                }
            },
            // Save button
            confirmButton = {
                Button(
                    onClick = {
                        makeEditedTask()
                        onSaveRequest(editedTask)
                        onDismissRequest()
                    }
                ) {
                    Text("Save")
                }
            },
            // Cancel button
            dismissButton = {
                OutlinedButton(onClick = onDismissRequest) {
                    Text("Cancel")
                }
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false,
            )
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksTable() {
    //val tasksManager = TasksManager.getInstance()
    val tasksArray = tasksManager.getTasksArray()
    var tasks by remember { mutableStateOf(emptyList<JsonObject>()) }
    var showLoadStrategyDialog by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var loadResult by remember { mutableStateOf<Boolean?>(null) }
    val coroutineScope = rememberCoroutineScope()
    var installedVersions by remember { mutableStateOf(mutableMapOf<String, String>()) }
    var latestVersions by remember { mutableStateOf(mutableMapOf<String, String>()) }

    // Load tasks when component is first displayed
    LaunchedEffect(tasksArray) {
        tasks = buildList {
            if (tasksArray != null) {
                for (i in 0 until tasksArray.size()) {
                    add(tasksArray.get(i).asJsonObject)
                }
            }
        }
        // Check installed versions for all tasks
        coroutineScope.launch {
            val versions = mutableMapOf<String, String>()
            val latest = mutableMapOf<String, String>()
            tasks.forEach { task ->
                val name = task.get("name")?.asString ?: "unknown"
                logger.d("TasksManager", "Checking installed version for $name")
                // Check latest version
                val installType = task.get("install_type")?.asString ?: ""
                if (installType == StaticSettings.InstallTypes.GITHUB) {
                    val githubObj = task.getAsJsonObject(StaticSettings.InstallTypes.GITHUB)
                    val apiUrl = githubObj.get("api_url").asString ?: ""
                    if (apiUrl.isNotEmpty()) {
                        val latestObj = tasksManager.getLatestGithubRelease(apiUrl)
                        latest[name] = latestObj?.get("tag_name")?.asString ?: ""
                        logger.d("TasksTable.Versions", "Latest version for $name: ${latest[name]} with url: $apiUrl")
                    }
                }
                else  {
                    latest[name] = "none"
                }
                versions[name] = TaskUtils.checkCurrentVersion(task)
                logger.d("TasksTable.Versions", "Checked version for $name: ${versions[name]}")

            }
            installedVersions = versions
            latestVersions = latest
        }
    }

    // Function to update task list after any operation
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
        // Update installed versions after refreshing tasks
        coroutineScope.launch {
            val versions = mutableMapOf<String, String>()
            tasks.forEach { task ->
                val name = task.get("name")?.asString ?: "unknown"
                versions[name] = TaskUtils.checkCurrentVersion(task)
            }
            installedVersions = versions
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        // Header
        Text(
            "Task List",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        // Refresh tasks button
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = { showLoadStrategyDialog = true },
                enabled = !isLoading
            ) {
                Text("Download task list")
            }
            Button(
                onClick = { refreshTasks() },
            ) {
                Icon(ICON_REFRESH, contentDescription = "Refresh")
                Spacer(Modifier.width(4.dp))
                Text("Refresh")
            }
            Button(
                onClick = { showAddDialog = true }
            ) {
                Icon(ICON_ADD, contentDescription = "Add")
                Spacer(Modifier.width(4.dp))
                Text("Add")
            }
            // Show loading indicator
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            // Show loading result
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

        // Tasks table
        if (tasks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Tasks list are missing, pls download it")
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Define constant relative sizes for columns
                val nameWidth = 0.10f
                val descriptionWidth = 0.25f
                val installTypeWidth = 0.10f
                val versionWidth = 0.09f
                //val latestVersionWidth = 0.08f
                val installedVersionWidth = 0.09f
                val statusWidth = 0.05f
                val installWidth = 0.14f
                val actionsWidth = 0.10f

                // Table headers
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Name",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(nameWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Install",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(installWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Description",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(descriptionWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Install Type",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(installTypeWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Selected Version",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(versionWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
//                        Text(
//                            "Latest Version",
//                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
//                            modifier = Modifier.weight(latestVersionWidth),
//                            color = MaterialTheme.colorScheme.onSurfaceVariant
//                        )
                        Text(
                            "Installed Version",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(installedVersionWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Status",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(statusWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            "Actions",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            modifier = Modifier.weight(actionsWidth),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                HorizontalDivider()
                // Tasks list
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(tasks) { task ->
                        val taskName = task.get("name")?.asString ?: "Unknown name"
                        val taskInstallType = task.get("install_type")?.asString ?: "Unknown install type"

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                //.padding(horizontal = 5.dp, vertical = 5.dp)
                                .clickable {},
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Name
                            Row(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(nameWidth),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {


                                if (taskInstallType == StaticSettings.InstallTypes.GITHUB) {
                                    val githubObj = task.getAsJsonObject(StaticSettings.InstallTypes.GITHUB)
                                    val githubUrl = githubObj.get("url")?.asString ?: ""
                                    val interactionSource = remember { MutableInteractionSource() }
                                    val isHovered by interactionSource.collectIsHoveredAsState()

                                    Text(
                                        text = task.get("name")?.asString ?: "Unknown name",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (isHovered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        modifier = Modifier
                                            .padding(start = 12.dp, end = 12.dp)
                                            .clickable(
                                                enabled = githubUrl.isNotEmpty(),
                                                interactionSource = interactionSource,
                                                indication = null // Це прибирає ефект ripple
                                            ) {
                                                if (githubUrl.isNotEmpty()) {
                                                    try {
                                                        val url = URI(githubUrl).toURL()
                                                        Desktop.getDesktop().browse(url.toURI())
                                                    } catch (e: Exception) {
                                                        logger.e("TasksTable", "Error opening URL:", e)
                                                    }
                                                }
                                            }
                                    )

                                } else {
                                    Text(
                                        text = task.get("name")?.asString ?: "Unknown name",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier
                                            .padding(start = 12.dp, end = 12.dp),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            // Install
                            Row(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(installWidth),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if ((task.get("install_type")?.asString == StaticSettings.InstallTypes.GITHUB
                                            || task.get("install_type")?.asString == StaticSettings.InstallTypes.PACKAGE)
                                    && task.get("enabled")?.asBoolean == true) {
                                    InstallButton(
                                        task,
                                        onInstallComplete = { refreshTasks() }
                                    )
                                }
                            }
                            // Description
                            Text(
                                text = task.get("description")?.asString ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(descriptionWidth),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Install Type
                            Text(
                                text = task.get("install_type")?.asString ?: "",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(installTypeWidth),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Version
                            val installVersion = task.get("install_version")?.asString ?: ""
                            val latestVersion = latestVersions[taskName] ?: ""
                            var annotatedString = ""
                            if (latestVersion != installVersion && latestVersion != "none") {
                                annotatedString = latestVersion
                            }
                            Text(
                                //text = installVersion,
                                text = buildAnnotatedString {
                                    append(installVersion)
                                    withStyle(
                                        style = SpanStyle(
                                            baselineShift = BaselineShift.Superscript,
                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.7f,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        append(" $annotatedString")
                                    }
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(versionWidth),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = when {
                                    latestVersion != installVersion && latestVersion != "none" && latestVersion != "" -> MaterialTheme.colorScheme.error
                                    else -> MaterialTheme.colorScheme.primary
                                }

                            )
//                            // latest version
//                            Text(
//                                //text = latestVersion,
//
//                                text = buildAnnotatedString {
//                                    append(latestVersion)
//                                    withStyle(
//                                        style = SpanStyle(
//                                            baselineShift = BaselineShift.Superscript,
//                                            fontSize = MaterialTheme.typography.bodyMedium.fontSize * 0.7f
//                                        )
//                                    ) {
//                                        append(latestVersion)
//                                    }
//                                },
//
//                                style = MaterialTheme.typography.bodyMedium,
//                                modifier = Modifier
//                                    .padding(start = 12.dp, end = 12.dp )
//                                    .weight(latestVersionWidth),
//                                maxLines = 1,
//                                overflow = TextOverflow.Ellipsis,
//                                color = when {
//                                    latestVersion == "Checking..." -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
//                                    latestVersion != installVersion && latestVersion != "none" -> MaterialTheme.colorScheme.error
//                                    latestVersion.startsWith("none") -> MaterialTheme.colorScheme.tertiary
//                                    else -> MaterialTheme.colorScheme.primary
//                                }
//                            )
                            // Installed Version from TaskUtils.checkCurrentVersion
                            val currentVersion = installedVersions[taskName] ?: "Checking..."
                            Text(
                                text = currentVersion,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(installedVersionWidth),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = when {
                                    currentVersion == "Checking..." -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                    currentVersion.startsWith("Not installed") -> MaterialTheme.colorScheme.error
                                    currentVersion.startsWith("Check error") -> MaterialTheme.colorScheme.error
                                    currentVersion.startsWith("Check disabled") -> MaterialTheme.colorScheme.tertiary
                                    else -> MaterialTheme.colorScheme.primary
                                }
                            )

                            // Status
                            var enabled by remember { mutableStateOf(task.get("enabled")?.asBoolean ?: false) }
                            Box(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(statusWidth),
                                contentAlignment = Alignment.Center
                            ) {
                                Checkbox(
                                    checked = enabled,
                                    onCheckedChange = { isChecked ->
                                        enabled = isChecked
                                        task.addProperty("enabled", isChecked)
                                        val name = task.get("name")?.asString ?: return@Checkbox
                                        tasksManager.updateTask(name, task)
                                    }
                                )
                            }

                            // Actions
                            var showDeleteConfirmation by remember { mutableStateOf(false) }
                            var showEditDialog by remember { mutableStateOf(false) }

                            Row(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 12.dp )
                                    .weight(actionsWidth),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Edit button
                                IconButton(onClick = { showEditDialog = true }) {
                                    Icon(ICON_EDIT, contentDescription = "Edit")
                                }
                                // Delete button
                                IconButton(onClick = { showDeleteConfirmation = true }) {
                                    Icon(ICON_DELETE, contentDescription = "Delete")
                                }
                            }

                            // Dialog windows
                            //val taskName = task.get("name")?.asString ?: "Unknown task"

                            ConfirmationDialog(
                                isOpen = showDeleteConfirmation,
                                onDismissRequest = { showDeleteConfirmation = false },
                                onConfirm = {
                                    val name = task.get("name")?.asString ?: return@ConfirmationDialog
                                    tasksManager.removeTask(name)
                                    //refreshTasksList()
                                    refreshTasks()
                                },
                                title = "Delete Confirmation",
                                text = "Are you sure you want to delete the task \"$taskName\"?",
                                confirmButtonText = "Delete",
                                dismissButtonText = "Cancel"
                            )

                            if (showEditDialog) {
                                TaskEditDialog(
                                    isOpen = showEditDialog,
                                    task = task,
                                    onDismissRequest = { showEditDialog = false },
                                    onSaveRequest = { updatedTask ->
                                        val name = task.get("name")?.asString ?: return@TaskEditDialog
                                        val result = tasksManager.updateTask(name, updatedTask)
                                        if (result) {
                                            //refreshTasksList()
                                            refreshTasks()
                                            showEditDialog = false
                                        }
                                    }
                                )
                            }
                        }
                        HorizontalDivider()
                    }
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
            isLoading = true
            loadResult = null

            coroutineScope.launch {
                val success = tasksManager.reloadTasks(strategy)
                isLoading = false
                loadResult = success

                if (success) {
                    //refreshTasksList()
                    refreshTasks()
                }
            }
        }
    )
}