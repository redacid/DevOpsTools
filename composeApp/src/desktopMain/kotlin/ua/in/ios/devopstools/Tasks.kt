
package ua.`in`.ios.devopstools

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.io.IOException

class TasksManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val homeDir: String = System.getProperty("user.home")
    private val configDir: String = "$homeDir/.devopstools"
    private val tasksFile: String = "$configDir/tasks.json"
    private var tasks: JsonObject = JsonObject()
    private val settingsManager = SettingsManager.getInstance()

    /**
     * Enum для визначення стратегії завантаження завдань
     */
    enum class LoadStrategy {
        REPLACE_ALL,      // Замінити всі завдання новими
        ADD_TO_EXISTING,  // Додати нові завдання до існуючих
        ADD_MISSING,      // Додати тільки ті завдання, яких немає за назвою
        UPDATE_AND_ADD    // Оновити існуючі завдання та додати нові
    }

    init {
        loadTasks()
    }

    private fun loadTasks() {
        // Перевіряємо чи існує директорія конфігурації
        val configDirPath = Paths.get(configDir)
        if (!Files.exists(configDirPath)) {
            Files.createDirectories(configDirPath)
        }

        // Перевіряємо чи існує файл завдань
        val tasksFilePath = File(tasksFile)
        if (!tasksFilePath.exists()) {
            // Якщо файл не існує, завантажуємо його з URL, заданого в налаштуваннях
            downloadTasksFromUrl()
        }

        // Завантажуємо завдання з файлу
        try {
            FileReader(tasksFile).use { reader ->
                tasks = JsonParser.parseReader(reader).asJsonObject
            }
            updateGitHubApiUrls()

        } catch (e: Exception) {
            println("Помилка завантаження завдань: ${e.message}")
            // Створюємо порожній об'єкт завдань, якщо сталася помилка
            createEmptyTasks()
        }
    }

    /**
     * Завантажує завдання з URL з вказаною стратегією завантаження
     * @param strategy Стратегія завантаження завдань
     * @return true якщо завантаження успішне, false у випадку помилки
     */
    fun reloadTasks(strategy: LoadStrategy = LoadStrategy.REPLACE_ALL): Boolean {
        try {
            val tasksUrl = settingsManager.getString("settings.tasks_url")

            // Змінюємо URL GitHub, щоб отримати raw вміст
            val rawUrl = if (tasksUrl.contains("github.com") && !tasksUrl.contains("raw.githubusercontent.com")) {
                tasksUrl.replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/")
            } else {
                tasksUrl
            }

            println("Завантаження завдань з URL: $rawUrl із стратегією: $strategy")

            // Завантажуємо вміст файлу з URL
            val content = URL(rawUrl).readText()

            // Перевіряємо, чи вміст є валідним JSON
            val downloadedTasks = JsonParser.parseString(content).asJsonObject

            // Обробляємо завантажені завдання згідно вибраної стратегії
            when (strategy) {
                LoadStrategy.REPLACE_ALL -> {
                    // Повністю замінюємо завдання новими
                    tasks = downloadedTasks
                }
                LoadStrategy.ADD_TO_EXISTING -> {
                    // Додаємо нові завдання до існуючих
                    val currentTasksArray = getTasksArray() ?: JsonArray()
                    val downloadedTasksArray = downloadedTasks.getAsJsonArray("tasks") ?: JsonArray()

                    for (i in 0 until downloadedTasksArray.size()) {
                        currentTasksArray.add(downloadedTasksArray.get(i))
                    }
                }
                LoadStrategy.ADD_MISSING -> {
                    // Додаємо тільки ті завдання, яких немає за назвою
                    val currentTasksArray = getTasksArray() ?: JsonArray()
                    val downloadedTasksArray = downloadedTasks.getAsJsonArray("tasks") ?: JsonArray()

                    // Створюємо набір існуючих назв завдань
                    val existingTaskNames = mutableSetOf<String>()
                    for (i in 0 until currentTasksArray.size()) {
                        val task = currentTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null) {
                            existingTaskNames.add(name)
                        }
                    }

                    // Додаємо лише ті завдання, яких немає в існуючих
                    for (i in 0 until downloadedTasksArray.size()) {
                        val task = downloadedTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null && !existingTaskNames.contains(name)) {
                            currentTasksArray.add(task)
                        }
                    }
                }
                LoadStrategy.UPDATE_AND_ADD -> {
                    // Оновлюємо існуючі завдання та додаємо нові
                    val currentTasksArray = getTasksArray() ?: JsonArray()
                    val downloadedTasksArray = downloadedTasks.getAsJsonArray("tasks") ?: JsonArray()

                    // Створюємо мапу існуючих завдань за назвою
                    val existingTaskMap = mutableMapOf<String, Int>()
                    for (i in 0 until currentTasksArray.size()) {
                        val task = currentTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null) {
                            existingTaskMap[name] = i
                        }
                    }

                    // Оновлюємо існуючі або додаємо нові завдання
                    for (i in 0 until downloadedTasksArray.size()) {
                        val task = downloadedTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null) {
                            // Якщо завдання існує, оновлюємо його
                            if (existingTaskMap.containsKey(name)) {
                                val index = existingTaskMap[name] ?: continue
                                // Зберігаємо статус (enabled) з існуючого завдання
                                val existingTask = currentTasksArray.get(index).asJsonObject
                                val enabled = existingTask.get("enabled")?.asBoolean ?: false
                                task.addProperty("enabled", enabled)
                                currentTasksArray.set(index, task)
                            } else {
                                // Інакше додаємо нове завдання
                                currentTasksArray.add(task)
                            }
                        }
                    }
                }
            }

            // Зберігаємо оновлені завдання
            saveTasks()
            updateGitHubApiUrls()
            return true
        } catch (e: Exception) {
            println("Помилка завантаження завдань з URL: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    private fun downloadTasksFromUrl() {
        reloadTasks(LoadStrategy.REPLACE_ALL)
    }

    /**
     * Перевіряє та оновлює api_url для GitHub завдань
     */
    private fun updateGitHubApiUrls() {
        val tasksArray = getTasksArray() ?: return
        var updated = false

        for (i in 0 until tasksArray.size()) {
            val task = tasksArray.get(i).asJsonObject

            // Перевіряємо, чи це GitHub-завдання
            if (task.has("install_type") &&
                task.get("install_type").asString == "github" &&
                task.has("github")) {

                val githubObj = task.getAsJsonObject("github")

                // Перевіряємо, чи є url, але немає api_url
                if (githubObj.has("url") &&
                    (!githubObj.has("api_url") || githubObj.get("api_url").asString.isEmpty())) {

                    val githubUrl = githubObj.get("url").asString
                    val apiUrl = convertGithubUrlToApiUrl(githubUrl)

                    if (apiUrl.isNotEmpty()) {
                        githubObj.addProperty("api_url", apiUrl)
                        updated = true
                    }
                }
            }
        }

        // Зберігаємо зміни, якщо були оновлення
        if (updated) {
            saveTasks()
        }
    }

    /**
     * Конвертує звичайний GitHub URL у відповідний API URL
     */
    private fun convertGithubUrlToApiUrl(githubUrl: String): String {
        // Перевіряємо, чи це GitHub URL
        if (!githubUrl.contains("github.com")) {
            return ""
        }

        try {
            // Витягуємо шлях репозиторію з URL
            val regex = "https?://github.com/([^/]+/[^/]+).*".toRegex()
            val matchResult = regex.find(githubUrl)

            if (matchResult != null) {
                val repoPath = matchResult.groupValues[1].trim()
                // Видаляємо .git, якщо він є в кінці
                val cleanRepoPath = repoPath.replace("\\.git$".toRegex(), "")

                return "https://api.github.com/repos/$cleanRepoPath"
            }
        } catch (e: Exception) {
            println("Помилка при конвертації GitHub URL: ${e.message}")
        }

        return ""
    }

    private fun createEmptyTasks() {
        // Створюємо порожній об'єкт завдань з мінімальною структурою
        tasks = JsonObject().apply {
            add("tasks", JsonArray())
        }
        saveTasks()
    }

    fun saveTasks() {
        try {
            FileWriter(tasksFile).use { writer ->
                gson.toJson(tasks, writer)
            }
        } catch (e: Exception) {
            println("Помилка збереження завдань: ${e.message}")
        }
    }

    fun getTasks(): JsonObject {
        return tasks
    }

    fun getTasksArray(): JsonArray? {
        return if (tasks.has("tasks") && tasks.get("tasks").isJsonArray) {
            tasks.getAsJsonArray("tasks")
        } else {
            null
        }
    }

    fun getTaskByName(name: String): JsonObject? {
        val tasksArray = getTasksArray() ?: return null

        for (i in 0 until tasksArray.size()) {
            val task = tasksArray.get(i).asJsonObject
            if (task.has("name") && task.get("name").asString == name) {
                return task
            }
        }

        return null
    }

    fun addTask(task: JsonObject) {
        if (!tasks.has("tasks")) {
            tasks.add("tasks", JsonArray())
        }

        val tasksArray = tasks.getAsJsonArray("tasks")
        tasksArray.add(task)
        saveTasks()
    }

    fun removeTask(name: String): Boolean {
        val tasksArray = getTasksArray() ?: return false

        for (i in 0 until tasksArray.size()) {
            val task = tasksArray.get(i).asJsonObject
            if (task.has("name") && task.get("name").asString == name) {
                tasksArray.remove(i)
                saveTasks()
                return true
            }
        }

        return false
    }

    fun updateTask(name: String, updatedTask: JsonObject): Boolean {
        val tasksArray = getTasksArray() ?: return false

        for (i in 0 until tasksArray.size()) {
            val task = tasksArray.get(i).asJsonObject
            if (task.has("name") && task.get("name").asString == name) {
                tasksArray.set(i, updatedTask)
                saveTasks()
                return true
            }
        }

        return false
    }

    // Старий метод, залишений для сумісності
    fun reloadTasks() {
        reloadTasks(LoadStrategy.REPLACE_ALL)
    }


    /**
     * Gets available installation options for the current system and architecture
     * based on the given task.
     *
     * @param task The task for which to find installation options
     * @return Map of installation type to list of available patterns
     */
    fun getAvailableInstallationOptions(task: JsonObject): Map<String, List<String>> {
        val systemInfo = SystemInfo.getInstance()
        val settingsManager = SettingsManager.getInstance()
        val result = mutableMapOf<String, List<String>>()

        // Get task name to include in patterns
        val taskName = task.get("name")?.asString ?: ""
        if (taskName.isEmpty()) {
            return emptyMap()
        }

        // Build the path to patterns based on OS and architecture
        val osFamily = systemInfo.osFamily.toString().lowercase()
        val arch = systemInfo.osArch

        // Map Java architecture to our pattern architecture
        val mappedArch = when {
            arch.contains("amd64") || arch.contains("x86_64") -> "amd64"
            arch.contains("86") -> "386"
            arch.contains("arm64") || arch.contains("aarch64") -> "arm64"
            arch.contains("arm") -> "arm"
            else -> "amd64" // Default to amd64 if unknown
        }

        // Path to patterns in settings
        val patternsPath = "settings.application_patterns.os.$osFamily.arch.$mappedArch"

        // Get patterns object from settings
        val patternsObj = settingsManager.getObject(patternsPath)

        if (patternsObj != null) {
            // For each type of installation pattern
            patternsObj.keySet().forEach { installType ->
                if (patternsObj.has(installType) && patternsObj.get(installType).isJsonArray) {
                    val patterns = patternsObj.getAsJsonArray(installType)
                    val patternList = mutableListOf<String>()

                    // Insert task name pattern for each pattern
                    for (i in 0 until patterns.size()) {
                        if (patterns.get(i).isJsonPrimitive) {
                            val pattern = patterns.get(i).asString

                            // Create pattern with separate task name segment
                            // Format: *taskName*rest_of_pattern
                            val modifiedPattern = "*$taskName$pattern"

                            patternList.add(modifiedPattern)
                        }
                    }

                    result[installType] = patternList
                }
            }
        }

        // Add special case for Linux distributions if needed
        if (osFamily == "linux") {
            // Determine if we need deb or rpm based on the distribution
            if (systemInfo.supportsDeb) {
                result["package_type"] = listOf("deb_based")
            } else if (systemInfo.supportsRpm) {
                result["package_type"] = listOf("rpm_based")
            }
        }

        return result
    }

    /**
     * Finds the most appropriate installation file pattern for the current system
     * based on the task name and available release assets.
     *
     * @param taskName The name of the task
     * @param assetNames List of available asset file names
     * @return The best matching asset name or null if no match found
     */
    fun findBestInstallationAsset(taskName: String, assetNames: List<String>): String? {
        val systemInfo = SystemInfo.getInstance()
        val settingsManager = SettingsManager.getInstance()

        // Create a dummy task object to get installation options
        val dummyTask = JsonObject().apply {
            addProperty("name", taskName)
        }

        val installOptions = getAvailableInstallationOptions(dummyTask)

        // Create a scoring system for each asset
        val assetScores = mutableMapOf<String, Int>()

        // Initialize scores
        assetNames.forEach { assetScores[it] = 0 }

        // Score based on our installation type preferences
        val preferredOrder = listOf("deb_based", "rpm_based", "package", "binary")

        // Check each pattern type
        installOptions.forEach { (installType, patterns) ->
            // Get preference score for this type (higher is better)
            val typeScore = preferredOrder.indexOf(installType).let {
                if (it == -1) 0 else (preferredOrder.size - it) * 100
            }

            // Check each pattern against each asset
            patterns.forEach { pattern ->
                // Convert pattern to regex
                // Replace dots with escaped dots and * with regex wildcard
                val regexPattern = pattern.replace(".", "\\.").replace("*", ".*")
                val regex = regexPattern.toRegex(RegexOption.IGNORE_CASE)

                assetNames.forEach { asset ->
                    if (regex.matches(asset)) {
                        // Add score for this match
                        assetScores[asset] = assetScores[asset]!! + typeScore + 10

                        // Bonus points for exact architecture match
                        when {
                            systemInfo.osArch.contains("64") && asset.contains("64") ->
                                assetScores[asset] = assetScores[asset]!! + 5
                            systemInfo.osArch.contains("86") && !asset.contains("64") && asset.contains("86") ->
                                assetScores[asset] = assetScores[asset]!! + 5
                        }

                        // Bonus points for containing the task name
                        if (asset.contains(taskName, ignoreCase = true)) {
                            assetScores[asset] = assetScores[asset]!! + 3
                        }
                    }
                }
            }
        }

        // Debug log to see all scores
        println("Asset scores for task '$taskName':")
        assetScores.filter { it.value > 0 }.forEach { (asset, score) ->
            println("  $asset: $score")
        }

        // Get the asset with the highest score, if any scored above 0
        return assetScores.filter { it.value > 0 }
            .maxByOrNull { it.value }
            ?.key
    }
    companion object {
        @Volatile
        private var instance: TasksManager? = null

        fun getInstance(): TasksManager {
            return instance ?: synchronized(this) {
                instance ?: TasksManager().also { instance = it }
            }
        }
    }
}