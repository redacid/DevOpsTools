package ua.`in`.ios.devopstools

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

class SettingsManager {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val homeDir: String = System.getProperty("user.home")
    private val configDir: String = "$homeDir/.devopstools"
    private val configFile: String = "$configDir/settings.json"
    private var settings: JsonObject = JsonObject()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        // Перевіряємо чи існує директорія конфігурації
        val configDirPath = Paths.get(configDir)
        if (!Files.exists(configDirPath)) {
            Files.createDirectories(configDirPath)
        }

        // Перевіряємо чи існує файл конфігурації
        val configFilePath = File(configFile)
        if (!configFilePath.exists()) {
            // Створюємо файл з дефолтними налаштуваннями
            createDefaultSettings()
        }

        // Завантажуємо налаштування
        try {
            FileReader(configFile).use { reader ->
                settings = JsonParser.parseReader(reader).asJsonObject
            }
        } catch (e: Exception) {
            println("Помилка завантаження налаштувань: ${e.message}")
            createDefaultSettings()
        }
    }

    private fun createDefaultSettings() {
        // Дефолтні налаштування
        val defaultSettings = JsonObject().apply {
            val settingsObj = JsonObject().apply {
                addProperty("install_path", "/usr/bin")
                addProperty("temp_path", "/tmp")
                addProperty("tasks_url", "https://github.com/redacid/DevOpsTools/tasks.json")

                // install_types масив
                val installTypesArray = gson.toJsonTree(arrayOf(
                    "github",
                    "distributive_package",
                    "package",
                    "remote_shell_script",
                    "shell_cmd",
                    "package_manager"
                )).asJsonArray
                add("install_types", installTypesArray)

                // application_patterns
                val applicationPatternsObj = JsonObject().apply {
                    val osObj = JsonObject().apply {
                        val linuxObj = JsonObject().apply {
                            val archObj = JsonObject().apply {
                                // amd64
                                val amd64Obj = JsonObject().apply {
                                    val debBasedArray = gson.toJsonTree(arrayOf(
                                        "*amd64.deb",
                                        "*x86_64.deb"
                                    )).asJsonArray
                                    add("deb_based", debBasedArray)

                                    val rpmBasedArray = gson.toJsonTree(arrayOf(
                                        "*amd64.rpm",
                                        "*x86_64.rpm"
                                    )).asJsonArray
                                    add("rpm_based", rpmBasedArray)

                                    val packageArray = gson.toJsonTree(arrayOf(
                                        "*linux*amd64.tar.gz",
                                        "*linux*x86_64.tar.gz"
                                    )).asJsonArray
                                    add("package", packageArray)

                                    val binaryArray = gson.toJsonTree(arrayOf(
                                        "*linux*amd64",
                                        "*linux*x86_64"
                                    )).asJsonArray
                                    add("binary", binaryArray)
                                }
                                add("amd64", amd64Obj)

                                // 386
                                val i386Obj = JsonObject().apply {
                                    val debBasedArray = gson.toJsonTree(arrayOf(
                                        "*386.deb"
                                    )).asJsonArray
                                    add("deb_based", debBasedArray)

                                    val rpmBasedArray = gson.toJsonTree(arrayOf(
                                        "*386.rpm"
                                    )).asJsonArray
                                    add("rpm_based", rpmBasedArray)

                                    val packageArray = gson.toJsonTree(arrayOf(
                                        "*linux*386.tar.gz"
                                    )).asJsonArray
                                    add("package", packageArray)

                                    val binaryArray = gson.toJsonTree(arrayOf(
                                        "*linux*386"
                                    )).asJsonArray
                                    add("binary", binaryArray)
                                }
                                add("386", i386Obj)
                            }
                            add("arch", archObj)
                        }
                        add("linux", linuxObj)
                    }
                    add("os", osObj)
                }
                add("application_patterns", applicationPatternsObj)

                // package_managers
                val packageManagersObj = JsonObject().apply {
                    val aptObj = JsonObject().apply {
                        addProperty("update_cmd", "update")
                        addProperty("install_cmd", "install")
                        addProperty("remove_cmd", "remove")
                    }
                    add("apt", aptObj)

                    val yumObj = JsonObject().apply {
                        addProperty("install_cmd", "install")
                        addProperty("remove_cmd", "remove")
                    }
                    add("yum", yumObj)

                    val dnfObj = JsonObject().apply {
                        addProperty("install_cmd", "install")
                        addProperty("remove_cmd", "remove")
                    }
                    add("dnf", dnfObj)
                }
                add("package_managers", packageManagersObj)

                // package_installers
                val packageInstallersObj = JsonObject().apply {
                    val dpkgObj = JsonObject().apply {
                        addProperty("install_cmd", "--install")
                        addProperty("remove_cmd", "--remove")
                    }
                    add("dpkg", dpkgObj)

                    val rpmObj = JsonObject().apply {
                        addProperty("install_cmd", "--install")
                        addProperty("remove_cmd", "--erase")
                    }
                    add("rpm", rpmObj)
                }
                add("package_installers", packageInstallersObj)
            }
            add("settings", settingsObj)
        }

        // Зберігаємо дефолтні налаштування
        settings = defaultSettings
        saveSettings()
    }

    fun saveSettings() {
        try {
            FileWriter(configFile).use { writer ->
                gson.toJson(settings, writer)
            }
        } catch (e: Exception) {
            println("Помилка збереження налаштувань: ${e.message}")
        }
    }

    fun getSettings(): JsonObject {
        return settings
    }

    fun getString(path: String, defaultValue: String = ""): String {
        val parts = path.split(".")
        var current = settings

        for (i in 0 until parts.size - 1) {
            if (current.has(parts[i]) && current.get(parts[i]).isJsonObject) {
                current = current.getAsJsonObject(parts[i])
            } else {
                return defaultValue
            }
        }

        val lastKey = parts.last()
        return if (current.has(lastKey) && current.get(lastKey).isJsonPrimitive) {
            current.get(lastKey).asString
        } else {
            defaultValue
        }
    }

    fun setString(path: String, value: String) {
        val parts = path.split(".")
        var current = settings

        for (i in 0 until parts.size - 1) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject) {
                current.add(parts[i], JsonObject())
            }
            current = current.getAsJsonObject(parts[i])
        }

        current.addProperty(parts.last(), value)
        saveSettings()
    }

    // Отримання масиву рядків за шляхом
    fun getStringArray(path: String): List<String> {
        val parts = path.split(".")
        var current = settings

        for (i in 0 until parts.size - 1) {
            if (current.has(parts[i]) && current.get(parts[i]).isJsonObject) {
                current = current.getAsJsonObject(parts[i])
            } else {
                return emptyList()
            }
        }

        val lastKey = parts.last()
        return if (current.has(lastKey) && current.get(lastKey).isJsonArray) {
            val jsonArray = current.getAsJsonArray(lastKey)
            val result = mutableListOf<String>()
            for (i in 0 until jsonArray.size()) {
                if (jsonArray.get(i).isJsonPrimitive) {
                    result.add(jsonArray.get(i).asString)
                }
            }
            result
        } else {
            emptyList()
        }
    }

    // Отримання JsonObject за шляхом
    fun getObject(path: String): JsonObject? {
        val parts = path.split(".")
        var current = settings

        for (i in 0 until parts.size - 1) {
            if (current.has(parts[i]) && current.get(parts[i]).isJsonObject) {
                current = current.getAsJsonObject(parts[i])
            } else {
                return null
            }
        }

        val lastKey = parts.last()
        return if (current.has(lastKey) && current.get(lastKey).isJsonObject) {
            current.getAsJsonObject(lastKey)
        } else {
            null
        }
    }

    // Встановлення масиву рядків за шляхом
    fun setStringArray(path: String, values: List<String>) {
        val parts = path.split(".")
        var current = settings

        for (i in 0 until parts.size - 1) {
            if (!current.has(parts[i]) || !current.get(parts[i]).isJsonObject) {
                current.add(parts[i], JsonObject())
            }
            current = current.getAsJsonObject(parts[i])
        }

        val jsonArray = gson.toJsonTree(values).asJsonArray
        current.add(parts.last(), jsonArray)
        saveSettings()
    }

    companion object {
        @Volatile
        private var instance: SettingsManager? = null

        fun getInstance(): SettingsManager {
            return instance ?: synchronized(this) {
                instance ?: SettingsManager().also { instance = it }
            }
        }
    }
}