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

    private fun downloadTasksFromUrl() {
        try {
            val tasksUrl = settingsManager.getString("settings.tasks_url")

            // Змінюємо URL GitHub, щоб отримати raw вміст
            val rawUrl = if (tasksUrl.contains("github.com") && !tasksUrl.contains("raw.githubusercontent.com")) {
                tasksUrl.replace("github.com", "raw.githubusercontent.com")
                    .replace("/blob/", "/")
            } else {
                tasksUrl
            }

            println("Завантаження завдань з URL: $rawUrl")

            // Завантажуємо вміст файлу з URL
            val content = URL(rawUrl).readText()

            // Перевіряємо, чи вміст є валідним JSON
            val jsonObject = JsonParser.parseString(content).asJsonObject

            // Зберігаємо завантажені завдання у файл
            FileWriter(tasksFile).use { writer ->
                gson.toJson(jsonObject, writer)
            }

            tasks = jsonObject
        } catch (e: Exception) {
            println("Помилка завантаження завдань з URL: ${e.message}")
            e.printStackTrace()
            // Створюємо порожній об'єкт завдань, якщо сталася помилка
            createEmptyTasks()
        }
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

    fun reloadTasks() {
        downloadTasksFromUrl()
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