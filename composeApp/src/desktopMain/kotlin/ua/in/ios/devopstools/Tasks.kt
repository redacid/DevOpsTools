
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