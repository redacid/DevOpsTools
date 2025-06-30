package ua.`in`.ios.devopstools

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
            logger.e("TasksManager", "Помилка завантаження завдань", e)
            //println("Помилка завантаження завдань: ${e.message}")
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
            logger.i("TasksManager", "Завантаження завдань з URL: $rawUrl із стратегією: $strategy")
            //println("Завантаження завдань з URL: $rawUrl із стратегією: $strategy")

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
            logger.e("TasksManager", "Помилка завантаження завдань з URL", e)
            //println("Помилка завантаження завдань з URL: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    /**
     * Loads tasks from URL with specified loading strategy
     * @param strategy Tasks loading strategy
     * @return true if loading is successful, false in case of error
     */
    fun reloadTasks2(strategy: LoadStrategy = LoadStrategy.REPLACE_ALL): Boolean {
        try {
            val tasksUrl = settingsManager.getString("settings.tasks_url")

            // Change GitHub URL to get raw content
            val rawUrl = if (tasksUrl.contains("github.com") && !tasksUrl.contains("raw.githubusercontent.com")) {
                tasksUrl.replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/")
            } else {
                tasksUrl
            }
            logger.i("TasksManager", "Loading tasks from URL: $rawUrl with strategy: $strategy")
            //println("Loading tasks from URL: $rawUrl with strategy: $strategy")

            // Load content from URL considering possible token
            val content = if (tasksUrl.contains("github.com") || tasksUrl.contains("api.github.com")) {
                val response = fetchFromGithubApi(rawUrl)
                response ?: return false
            } else {
                URL(rawUrl).readText()
            }

            // Check if content is valid JSON
            val downloadedTasks = JsonParser.parseString(content).asJsonObject

            // Process downloaded tasks according to selected strategy
            when (strategy) {
                LoadStrategy.REPLACE_ALL -> {
                    // Completely replace tasks with new ones
                    tasks = downloadedTasks
                }
                LoadStrategy.ADD_TO_EXISTING -> {
                    // Add new tasks to existing ones
                    val currentTasksArray = getTasksArray() ?: JsonArray()
                    val downloadedTasksArray = downloadedTasks.getAsJsonArray("tasks") ?: JsonArray()

                    for (i in 0 until downloadedTasksArray.size()) {
                        currentTasksArray.add(downloadedTasksArray.get(i))
                    }
                }
                LoadStrategy.ADD_MISSING -> {
                    // Add only tasks that don't exist by name
                    val currentTasksArray = getTasksArray() ?: JsonArray()
                    val downloadedTasksArray = downloadedTasks.getAsJsonArray("tasks") ?: JsonArray()

                    // Create set of existing task names
                    val existingTaskNames = mutableSetOf<String>()
                    for (i in 0 until currentTasksArray.size()) {
                        val task = currentTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null) {
                            existingTaskNames.add(name)
                        }
                    }

                    // Add only tasks that don't exist in current set
                    for (i in 0 until downloadedTasksArray.size()) {
                        val task = downloadedTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null && !existingTaskNames.contains(name)) {
                            currentTasksArray.add(task)
                        }
                    }
                }
                LoadStrategy.UPDATE_AND_ADD -> {
                    // Update existing tasks and add new ones
                    val currentTasksArray = getTasksArray() ?: JsonArray()
                    val downloadedTasksArray = downloadedTasks.getAsJsonArray("tasks") ?: JsonArray()

                    // Create map of existing tasks by name
                    val existingTaskMap = mutableMapOf<String, Int>()
                    for (i in 0 until currentTasksArray.size()) {
                        val task = currentTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null) {
                            existingTaskMap[name] = i
                        }
                    }

                    // Update existing or add new tasks
                    for (i in 0 until downloadedTasksArray.size()) {
                        val task = downloadedTasksArray.get(i).asJsonObject
                        val name = task.get("name")?.asString
                        if (name != null) {
                            // If task exists, update it
                            if (existingTaskMap.containsKey(name)) {
                                val index = existingTaskMap[name] ?: continue
                                // Preserve status (enabled) from existing task
                                val existingTask = currentTasksArray.get(index).asJsonObject
                                val enabled = existingTask.get("enabled")?.asBoolean ?: false
                                task.addProperty("enabled", enabled)
                                currentTasksArray.set(index, task)
                            } else {
                                // Otherwise add new task
                                currentTasksArray.add(task)
                            }
                        }
                    }
                }
            }

            // Save updated tasks
            saveTasks()
            updateGitHubApiUrls()
            return true
        } catch (e: Exception) {
            logger.e("TasksManager", "Error loading tasks from URL", e)
            //println("Error loading tasks from URL: ${e.message}")
            e.printStackTrace()
            return false
        }
    }

    // Старий метод, залишений для сумісності
    fun reloadTasks() {
        reloadTasks(LoadStrategy.REPLACE_ALL)
    }

    private fun downloadTasksFromUrl() {
        reloadTasks(LoadStrategy.REPLACE_ALL)
    }

    /**
     * Converts a regular GitHub URL to the corresponding API URL
     */
    fun convertGithubUrlToApiUrl(githubUrl: String): String {
        // Check if it's a GitHub URL
        if (!githubUrl.contains("github.com")) {
            return ""
        }
        try {
            val regex = "https?://github\\.com/([\\w\\-\\.]+/[\\w\\-\\.]+)(?:\\.git|/.*)?".toRegex()
            val matchResult = regex.find(githubUrl)

            if (matchResult != null) {
                val repoPath = matchResult.groupValues[1].trim()
                // Remove .git if it's at the end (although our regex already handles this)
                val cleanRepoPath = repoPath.replace("\\.git$".toRegex(), "")
                logger.i("TasksManager", "Extracted GitHub repository path: $cleanRepoPath from URL: $githubUrl")
                //println("Extracted GitHub repository path: $cleanRepoPath from URL: $githubUrl")
                return "https://api.github.com/repos/$cleanRepoPath"
            } else {
                logger.w("TasksManager", "Failed to extract repository path from URL: $githubUrl")
            }
        } catch (e: Exception) {
            logger.e("TasksManager", "Error converting GitHub URL", e)
        }

        return ""
    }

    /**
     * Виконує HTTP запит до GitHub API з підтримкою авторизації
     *
     * @param apiUrl URL для запиту до GitHub API
     * @return Відповідь у вигляді рядка або null у випадку помилки
     */
    private fun fetchFromGithubApi(apiUrl: String): String? {
        val result = fetchFromGithubApiWithRateInfo(apiUrl)
        return result?.first
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
            logger.e("TasksManager", "Error saving tasks to file", e)
            //println("Помилка збереження завдань: ${e.message}")
        }
    }

//    fun getTasks(): JsonObject {
//        return tasks
//    }

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

    fun getTasksArray(): JsonArray? {
        return if (tasks.has("tasks") && tasks.get("tasks").isJsonArray) {
            tasks.getAsJsonArray("tasks")
        } else {
            null
        }
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

    /**
     * Checks and updates api_url for GitHub tasks
     */
    private fun updateGitHubApiUrls() {
        val tasksArray = getTasksArray() ?: return
        var updated = false

        for (i in 0 until tasksArray.size()) {
            val task = tasksArray.get(i).asJsonObject

            // Check if it's a GitHub task
            if (task.has("install_type") &&
                task.get("install_type").asString == "github" &&
                task.has("github")) {

                val githubObj = task.getAsJsonObject("github")

                // Check if there's a url but no api_url
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

        // Save changes if there were updates
        if (updated) {
            saveTasks()
        }
    }

    /**
     * Обробляє заголовки відповіді HTTP для отримання інформації про пагінацію та рейт-ліміти GitHub API
     *
     * @param connection HTTP з'єднання, з якого треба отримати заголовки
     * @return Пара (Map<String, String> з інформацією про пагінацію, Map<String, String> з інформацією про рейт-ліміти)
     */
    private fun processGitHubResponseHeaders(connection: java.net.HttpURLConnection): Pair<Map<String, String>, Map<String, String>> {
        val paginationInfo = mutableMapOf<String, String>()
        val rateLimitInfo = mutableMapOf<String, String>()

        // Обробка заголовку Link для пагінації
        val linkHeader = connection.getHeaderField("Link")
        if (linkHeader != null) {
            // Розбираємо заголовок Link на окремі посилання
            val linkPattern = "<([^>]*)>; rel=\"([^\"]*)\"".toRegex()
            val matches = linkPattern.findAll(linkHeader)

            for (match in matches) {
                val url = match.groupValues[1]
                val rel = match.groupValues[2]
                paginationInfo[rel] = url
            }

            logger.d("GitHub API-GRH", "Pagination links: ${paginationInfo.keys.joinToString()}")
        }

        // Обробка заголовків рейт-лімітів
        val rateLimit = connection.getHeaderField("X-RateLimit-Limit")
        val rateRemaining = connection.getHeaderField("X-RateLimit-Remaining")
        val rateReset = connection.getHeaderField("X-RateLimit-Reset")
        val rateUsed = connection.getHeaderField("X-RateLimit-Used")
        val rateResource = connection.getHeaderField("X-RateLimit-Resource")

        if (rateLimit != null) rateLimitInfo["limit"] = rateLimit
        if (rateRemaining != null) rateLimitInfo["remaining"] = rateRemaining
        if (rateReset != null) rateLimitInfo["reset"] = rateReset
        if (rateUsed != null) rateLimitInfo["used"] = rateUsed
        if (rateResource != null) rateLimitInfo["resource"] = rateResource

        // Логуємо інформацію про рейт-ліміти
        if (rateLimitInfo.isNotEmpty()) {
            val resetTime = if (rateReset != null) {
                val date = Date(rateReset.toLong() * 1000)
                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                sdf.format(date)
            } else "unknown"

            logger.i("GitHub API-GRH", "Rate limits: ${rateLimitInfo["remaining"] ?: "?"} / ${rateLimitInfo["limit"] ?: "?"} " +
                    "requests remaining. Reset at $resetTime (resource: ${rateLimitInfo["resource"] ?: "core"})")
        }

        return Pair(paginationInfo, rateLimitInfo)
    }

    /**
     * Виконує HTTP запит до GitHub API з підтримкою авторизації
     *
     * @param apiUrl URL для запиту до GitHub API
     * @return Трійка (String? з відповіддю, Map з інформацією про рейт-ліміти, Map з інформацією про пагінацію)
     *         або null у випадку помилки
     */
    private fun fetchFromGithubApiWithRateInfo(apiUrl: String): Triple<String?, Map<String, String>, Map<String, String>>? {
        try {
            val githubToken = settingsManager.getString("settings.github_token", "")
            val connection = URL(apiUrl).openConnection() as java.net.HttpURLConnection

            // Налаштування з'єднання
            connection.requestMethod = "GET"
            connection.setRequestProperty("Accept", "application/vnd.github.v3+json")
            connection.setRequestProperty("X-GitHub-Api-Version", "2022-11-28")

            // Додаємо токен авторизації, якщо доступний
            if (githubToken.isNotEmpty()) {
                connection.setRequestProperty("Authorization", "token $githubToken")
                logger.i("GitHub API", "Using GitHub token for API request to: $apiUrl")
            }

            // Перевіряємо код відповіді
            val responseCode = connection.responseCode

            // Отримуємо інформацію про заголовки
            val (paginationInfo, rateLimitInfo) = processGitHubResponseHeaders(connection)

            if (responseCode != 200) {
                logger.w("GitHub API", "Error when requesting GitHub API: HTTP $responseCode")
                logger.w("GitHub API", "Response: ${connection.responseMessage}")

                // Читаємо текст помилки, якщо доступний
                val errorStream = connection.errorStream
                if (errorStream != null) {
                    val errorText = errorStream.bufferedReader().use { it.readText() }
                    logger.w("GitHub API", "Error details: $errorText")
                }

                return Triple(null, rateLimitInfo, paginationInfo)
            }

            // Читаємо відповідь
            val response = connection.inputStream.bufferedReader().use { it.readText() }
            return Triple(response, rateLimitInfo, paginationInfo)
        } catch (e: Exception) {
            logger.e("GitHub API", "Error when requesting GitHub API", e)
            e.printStackTrace()
            return null
        }
    }

    /**
     * Отримує список релізів для GitHub завдання з підтримкою пагінації
     *
     * @param apiUrl URL до GitHub API для репозиторію
     * @param maxPages Максимальна кількість сторінок для завантаження (0 = всі доступні)
     * @return JsonArray з релізами або null у випадку помилки
     */
    fun getGithubReleases(apiUrl: String, maxPages: Int = 0): JsonArray? {
        try {
            val result = JsonArray()
            var currentUrl = "$apiUrl/releases"
            var currentPage = 1
            var hasMore = true

            while (hasMore && (maxPages == 0 || currentPage <= maxPages)) {
                logger.d("TaskManager.getGithubReleases", "Fetching releases page $currentPage: $currentUrl")

                // Виконуємо запит з отриманням інформації про рейт-ліміти та пагінацію
                val apiResponse = fetchFromGithubApiWithRateInfo(currentUrl) ?: return null
                val (response, rateLimitInfo, paginationLinks) = apiResponse

                if (response == null) {
                    logger.e("TaskManager.getGithubReleases", "Failed to fetch page $currentPage")
                    return if (result.size() > 0) result else null
                }

                // Перевіряємо залишок лімітів
                val remaining = rateLimitInfo["remaining"]?.toIntOrNull() ?: 0
                if (remaining <= 1) {
                    val resetTime = rateLimitInfo["reset"]?.toLongOrNull()
                    if (resetTime != null) {
                        val resetDate = Date(resetTime * 1000)
                        val now = Date()
                        val waitTimeMinutes = (resetDate.time - now.time) / 60000

                        logger.w("TaskManager.getGithubReleases", "Rate limit almost exhausted! " +
                                "Only $remaining requests left. Reset in ~$waitTimeMinutes minutes at " +
                                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(resetDate))

                        // Якщо ми вже щось отримали, краще повернути частковий результат, ніж нічого
                        if (result.size() > 0) {
                            logger.i("TaskManager.getGithubReleases", "Returning partial results to avoid rate limit issues")
                            hasMore = false
                            break
                        }
                    }
                }

                // Парсимо відповідь і додаємо до результату
                val pageReleases = JsonParser.parseString(response).asJsonArray
                for (i in 0 until pageReleases.size()) {
                    result.add(pageReleases.get(i))
                }

                logger.i("TaskManager.getGithubReleases", "Fetched ${pageReleases.size()} releases on page $currentPage, " +
                        "total so far: ${result.size()}")

                // Перевіряємо, чи є наступна сторінка
                if (paginationLinks.containsKey("next")) {
                    currentUrl = paginationLinks["next"] ?: break
                    currentPage++
                } else {
                    hasMore = false
                }
            }

            logger.i("TaskManager.getGithubReleases", "Successfully fetched ${result.size()} releases from GitHub")
            return result
        } catch (e: Exception) {
            logger.e("TaskManager.getGithubReleases", "Error getting releases from GitHub", e)
            return null
        }
    }

    /**
     * Gets latest release for a GitHub task
     *
     * @param apiUrl URL to GitHub API for repository
     * @return JsonObject with information about latest release or null in case of error
     */
    fun getLatestGithubRelease(apiUrl: String): JsonObject? {
        try {
            val latestReleaseUrl = "$apiUrl/releases/latest"
            val response = fetchFromGithubApi(latestReleaseUrl)

            if (response != null) {
                return JsonParser.parseString(response).asJsonObject
            }
        } catch (e: Exception) {
            logger.e("TasksManager", "Error getting latest release from GitHub", e)
            //println("Error getting latest release from GitHub: ${e.message}")
        }

        return null
    }

    /**
     * Gets information about specific release by its tag
     *
     * @param apiUrl URL to GitHub API for repository
     * @param tag Release tag
     * @return JsonObject with release information or null in case of error
     */
    fun getGithubReleaseByTag(apiUrl: String, tag: String): JsonObject? {
        try {
            val tagReleaseUrl = "$apiUrl/releases/tags/$tag"
            val response = fetchFromGithubApi(tagReleaseUrl)

            if (response != null) {
                return JsonParser.parseString(response).asJsonObject
            }
        } catch (e: Exception) {
            logger.e("TasksManager", "Error getting release for tag $tag from GitHub", e)
            //println("Error getting release for tag $tag from GitHub: ${e.message}")
        }

        return null
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
                    logger.d("TaskManager.getAvailableInstallationOptions", "$taskName Available $installType patterns: ${patternsObj.get(installType).asJsonArray}")

                    if (installType == "deb_based" && !systemInfo.supportsDeb) {
                        logger.w("TaskManager.getAvailableInstallationOptions", "deb based install options not supported")
                        return@forEach
                    }
                    if (installType == "rpm_based" && !systemInfo.supportsRpm) {
                        logger.w("TaskManager.getAvailableInstallationOptions", "rpm based install options not supported")
                        return@forEach
                    }

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
        logger.d("TasksManager", "Asset scores for task '$taskName'")
        //println("Asset scores for task '$taskName':")
        assetScores.filter { it.value > 0 }.forEach { (asset, score) ->
            logger.d("TasksManager", "  $asset: $score")
            //println("  $asset: $score")
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


/**
 * Utility class for tasks and applications management
 */
object TaskUtils {
    private val logger = Logger.getInstance()

    /**
     * Checks and returns the current version of an application installed in the system
     *
     * @param task The task for which to check the version
     * @return String with application version or status message ("Not installed", "Check error")
     */
    suspend fun checkCurrentVersion(task: JsonObject): String {
        return withContext(Dispatchers.IO) {
            try {
                val installType = getInstallTypeForTask(task)
                if (installType != null) {
                    val rawVersion = installType.getCurrentVersion(task)
                    if (rawVersion.isNotEmpty()) {
                        // Витягуємо версію за допомогою регулярних виразів
                        var extractedVersion = extractVersionNumber(rawVersion, task)
                        logger.d("TaskUtils.checkCurrentVersion", "Current version for ${task.get("name")}: $rawVersion")
                        return@withContext extractedVersion
                    } else {
                        "Not installed"
                    }
                } else {
                    logger.w("TaskUtils.checkCurrentVersion", "Could not determine installation type for task: ${task.get("name")?.asString ?: "unknown"}")
                    "Check error"
                }
            } catch (e: Exception) {
                logger.e("TaskUtils.checkCurrentVersion", "Error checking current version", e)
                "Check error: ${e.message}"
            }
        }
    }

    /**
     * Витягує номер версії з рядка виводу команди
     *
     * @param output Вихід команди версії
     * @param task Завдання для якого визначається версія
     * @return Номер версії або вихідний рядок, якщо версію не вдалося витягти
     */

    private fun extractVersionNumber(output: String, task: JsonObject): String {
        // Отримуємо патерн для розбору з завдання, якщо він є
        val versionPattern = task.get("version_pattern")?.asString

        if (versionPattern != null && versionPattern.isNotEmpty()) {
            try {
                val regex = versionPattern.toRegex()
                val matchResult = regex.find(output)
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    return matchResult.groupValues[1]
                }
            } catch (e: Exception) {
                logger.e("TaskUtils.extractVersionNumber", "Error parsing version with pattern: $versionPattern", e)
            }
        }

        // Перевірка форматів версій, які треба зберегти як є
        val standardVersionPatterns = listOf(
            "^v?(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)$".toRegex(), // Чисті версії: 1.2.3, v1.2.3, 1.2.3-beta, 1.2.3-beta.4
            "^v?(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)-?\\w*$".toRegex() // Версії з невеликим суфіксом: 1.2.3-1, v1.2.3-alpha
        )

        for (pattern in standardVersionPatterns) {
            try {
                val matchResult = pattern.find(output.trim())
                if (matchResult != null) {
                    // Якщо вивід команди вже є чистою версією - повертаємо її як є
                    return output.trim()
                }
            } catch (e: Exception) {
                continue
            }
        }

        // Витягуємо версію з тексту, якщо вона там є
        val versionExtractionPatterns = listOf(
            "v?(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)".toRegex(), // Стандартні версії в тексті
            "version\\s+(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)".toRegex(RegexOption.IGNORE_CASE), // version X.Y.Z
            "версія\\s+(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)".toRegex(RegexOption.IGNORE_CASE) // версія X.Y.Z (для локалізованих програм)
        )

        for (pattern in versionExtractionPatterns) {
            try {
                val matchResult = pattern.find(output)
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    val version = matchResult.groupValues[1]
                    // Перевіряємо, чи починається з "v" і повертаємо відповідно
                    return if (matchResult.groupValues[0].startsWith("v") && !version.startsWith("v")) {
                        "v$version"
                    } else {
                        version
                    }
                }
            } catch (e: Exception) {
                continue
            }
        }

        // Якщо нічого не знайдено, повертаємо обрізаний вивід (перший рядок)
        val firstLine = output.trim().split("\n").firstOrNull() ?: output
        // Обмежуємо довжину виводу до 50 символів, щоб уникнути дуже довгих рядків
        return if (firstLine.length > 50) firstLine.substring(0, 47) + "..." else firstLine
    }


    private fun extractVersionNumber2(output: String, task: JsonObject): String {
        // Отримуємо патерн для розбору з завдання, якщо він є
        val versionPattern = task.get("version_pattern")?.asString

        if (versionPattern != null && versionPattern.isNotEmpty()) {
            try {
                val regex = versionPattern.toRegex()
                val matchResult = regex.find(output)
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    return matchResult.groupValues[1]
                }
            } catch (e: Exception) {
                logger.e("TaskUtils.extractVersionNumber2", "Error parsing version with pattern: $versionPattern", e)
            }
        }

        // Використовуємо загальні патерни для витягування версії
        val commonPatterns = listOf(
            "v?(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)".toRegex(), // Стандартні версії: 1.2.3, v1.2.3, 1.2.3-beta, 1.2.3-beta.4
            "version\\s+(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)".toRegex(RegexOption.IGNORE_CASE), // version X.Y.Z
            "(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?).+".toRegex(), // Версія на початку рядка
            ".+(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?).+".toRegex(), // Версія всередині рядка
            ".+(\\d+\\.\\d+\\.\\d+(?:-\\w+(?:\\.\\d+)?)?)$".toRegex(), // Версія в кінці рядка
            "(\\d+\\.\\d+\\.\\d+)".toRegex() // Просто X.Y.Z
        )

        for (pattern in commonPatterns) {
            try {
                val matchResult = pattern.find(output)
                if (matchResult != null && matchResult.groupValues.size > 1) {
                    return matchResult.groupValues[1]
                }
            } catch (e: Exception) {
                // Продовжуємо перевірку з наступним патерном
                continue
            }
        }

        // Витягуємо перший набір чисел, розділених крапками
        val simplifiedPattern = "(\\d+(?:\\.\\d+)+)".toRegex()
        try {
            val matchResult = simplifiedPattern.find(output)
            if (matchResult != null && matchResult.groupValues.size > 1) {
                return matchResult.groupValues[1]
            }
        } catch (e: Exception) {
            logger.e("TaskUtils.extractVersionNumber2", "Error parsing version with simplified pattern", e)
        }

        // Якщо нічого не знайдено, повертаємо обрізаний вивід (перший рядок)
        val firstLine = output.trim().split("\n").firstOrNull() ?: output
        // Обмежуємо довжину виводу до 50 символів, щоб уникнути дуже довгих рядків
        return if (firstLine.length > 50) firstLine.substring(0, 47) + "..." else firstLine
    }


}
