package ua.`in`.ios.devopstools

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

/**
 * Інтерфейс для різних типів встановлення програм
 */
interface InstallType {
    /**
     * Назва типу встановлення, як вона зберігається в JSON
     */
    val typeName: String

    /**
     * Перевіряє, чи підтримується встановлення для поточної системи
     */
    suspend fun isSupported(): Boolean

    /**
     * Перевіряє, чи встановлена програма
     */
    suspend fun isInstalled(task: JsonObject): Boolean

    /**
     * Встановлює програму
     */
    suspend fun install(task: JsonObject): Boolean

    /**
     * Оновлює програму до нової версії
     */
    suspend fun update(task: JsonObject): Boolean

    /**
     * Видаляє програму
     */
    suspend fun uninstall(task: JsonObject): Boolean

    /**
     * Повертає поточну версію програми
     */
    suspend fun getCurrentVersion(task: JsonObject): String

    /**
     * Повертає доступну версію програми
     */
    suspend fun getAvailableVersion(task: JsonObject): String

    companion object {
        /**
         * Створює відповідний об'єкт InstallType на основі назви типу
         */
        fun createFromTypeName(typeName: String): InstallType {
            return when (typeName) {
                "github" -> GithubInstallType()
                "distributive_package" -> DistributivePackageInstallType()
                "package" -> PackageInstallType()
                "remote_shell_script" -> RemoteShellScriptInstallType()
                "shell_cmd" -> ShellCmdInstallType()
                "package_manager" -> PackageManagerInstallType()
                else -> throw IllegalArgumentException("Невідомий тип встановлення: $typeName")
            }
        }
    }
}

/**
 * Базова реалізація InstallType з загальними методами
 */
abstract class BaseInstallType : InstallType {
    /**
     * Виконує команду в системі
     */
    protected suspend fun executeCommand(command: String, workingDir: String = ""): Pair<Int, String> {
        return withContext(Dispatchers.IO) {
            try {
                val processBuilder = ProcessBuilder()
                if (workingDir.isNotEmpty()) {
                    processBuilder.directory(File(workingDir))
                }

                // Розділяємо команду на аргументи
                val args = command.split("\\s+".toRegex())
                processBuilder.command(args)

                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                Pair(exitCode, output)
            } catch (e: Exception) {
                logger.e("TasksManager", "Error when executing command '$command'", e)
                //println("Помилка при виконанні команди '$command': ${e.message}")
                Pair(-1, e.message ?: "Unknown error")
            }
        }
    }

    /**
     * Завантажує файл з URL
     */
    protected suspend fun downloadFile(url: String, destination: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val destFile = File(destination)
                if (!destFile.parentFile.exists()) {
                    destFile.parentFile.mkdirs()
                }

                URL(url).openStream().use { input ->
                    Files.copy(input, Paths.get(destination), StandardCopyOption.REPLACE_EXISTING)
                }
                true
            } catch (e: Exception) {
                logger.e("TasksManager", "Error when downloading file from '$url' to '$destination'", e)
                //println("Помилка при завантаженні файлу з '$url': ${e.message}")
                false
            }
        }
    }

    /**
     * Отримує шлях для встановлення з налаштувань або використовує типовий
     */
    protected fun getInstallPath(task: JsonObject): String {
        val settingsManager = SettingsManager.getInstance()
        val installPath = settingsManager.getString("settings.install_path")

        return if (installPath.isNotEmpty()) {
            installPath
        } else {
            "/usr/local/bin" // Типовий шлях
        }
    }
}

/**
 * Тип встановлення для GitHub релізів
 */
class GithubInstallType : BaseInstallType() {
    override val typeName = "github"

    override suspend fun isSupported(): Boolean = true

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val binaryName = task.get("binary_name")?.asString ?: return false
        val (exitCode, _) = executeCommand("which $binaryName")
        return exitCode == 0
    }
    override suspend fun install(task: JsonObject): Boolean {
        // Перевіряємо, чи є об'єкт github
        if (!task.has("github")) return false
        val githubObj = task.getAsJsonObject("github")

        // Отримуємо URL API
        val apiUrl = if (githubObj.has("api_url")) {
            githubObj.get("api_url").asString
        } else if (githubObj.has("url")) {
            // Конвертуємо звичайний URL в API URL
            val url = githubObj.get("url").asString
            convertGithubUrlToApiUrl(url)
        } else {
            return false
        }

        // Додаємо /releases до URL API
        val releasesUrl = "$apiUrl/releases"

        // Завантажуємо інформацію про релізи
        val releasesJson = try {
            URL(releasesUrl).readText()
        } catch (e: Exception) {
            logger.e("TasksManager", "Error when getting releases info from '$releasesUrl'", e)
            //println("Помилка при завантаженні інформації про релізи: ${e.message}")
            return false
        }

        // Тут буде логіка завантаження останнього релізу та встановлення бінарного файлу
        // В залежності від типу встановлення (binary, deb, rpm, etc.)
        // Цей код потребує додаткової імплементації

        return true
    }

    override suspend fun update(task: JsonObject): Boolean {
        // Аналогічно install, але з перевіркою поточної версії
        return install(task)
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        val binaryName = task.get("binary_name")?.asString ?: return false
        val installPath = getInstallPath(task)
        val binaryPath = "$installPath/$binaryName"

        return withContext(Dispatchers.IO) {
            try {
                val file = File(binaryPath)
                if (file.exists()) {
                    file.delete()
                }
                true
            } catch (e: Exception) {
                logger.e("TasksManager", "Error when deleting file '$binaryPath'", e)
                //println("Помилка при видаленні бінарного файлу: ${e.message}")
                false
            }
        }
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val binaryName = task.get("binary_name")?.asString ?: return ""
        val versionCmd = task.get("version_cmd")?.asString ?: "--version"

        val (exitCode, output) = executeCommand("$binaryName $versionCmd")
        return if (exitCode == 0) {
            // Витягуємо версію з виводу команди
            // Це потребує додаткової обробки для різних програм
            output.trim()
        } else {
            ""
        }
    }

    override suspend fun getAvailableVersion(task: JsonObject): String {
        // Перевіряємо, чи є об'єкт github
        if (!task.has("github")) return ""
        val githubObj = task.getAsJsonObject("github")

        // Отримуємо URL API
        val apiUrl = if (githubObj.has("api_url")) {
            githubObj.get("api_url").asString
        } else if (githubObj.has("url")) {
            // Конвертуємо звичайний URL в API URL
            val url = githubObj.get("url").asString
            convertGithubUrlToApiUrl(url)
        } else {
            return ""
        }

        // Додаємо /releases/latest до URL API
        val latestReleaseUrl = "$apiUrl/releases/latest"

        // Завантажуємо інформацію про останній реліз
        return try {
            val releaseJson = URL(latestReleaseUrl).readText()
            val jsonObject = com.google.gson.JsonParser.parseString(releaseJson).asJsonObject
            if (jsonObject.has("tag_name")) {
                jsonObject.get("tag_name").asString
            } else {
                ""
            }
        } catch (e: Exception) {
            logger.e("TasksManager", "Error when getting latest release info from '$latestReleaseUrl'", e)
            //println("Помилка при завантаженні інформації про останній реліз: ${e.message}")
            ""
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
            logger.e("TasksManager", "Error when converting github url '$githubUrl' to api url", e)
            //println("Помилка при конвертації GitHub URL: ${e.message}")
        }

        return ""
    }
}

/**
 * Тип встановлення для дистрибутивних пакетів (deb, rpm)
 */
class DistributivePackageInstallType : BaseInstallType() {
    override val typeName = "distributive_package"

    override suspend fun isSupported(): Boolean {
        // Перевіряємо, чи система підтримує deb або rpm
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        val (exitCodeRpm, _) = executeCommand("which rpm")
        return exitCodeDpkg == 0 || exitCodeRpm == 0
    }

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Перевіряємо deb-пакети через dpkg
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        if (exitCodeDpkg == 0) {
            val (exitCode, _) = executeCommand("dpkg -s $packageName")
            if (exitCode == 0) return true
        }

        // Перевіряємо rpm-пакети через rpm
        val (exitCodeRpm, _) = executeCommand("which rpm")
        if (exitCodeRpm == 0) {
            val (exitCode, _) = executeCommand("rpm -q $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun install(task: JsonObject): Boolean {
        // Логіка для завантаження та встановлення deb/rpm пакета
        // Потребує додаткової імплементації
        return false
    }

    override suspend fun update(task: JsonObject): Boolean {
        return install(task)
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Видаляємо deb-пакети через dpkg
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        if (exitCodeDpkg == 0) {
            val (exitCode, _) = executeCommand("sudo dpkg -r $packageName")
            if (exitCode == 0) return true
        }

        // Видаляємо rpm-пакети через rpm
        val (exitCodeRpm, _) = executeCommand("which rpm")
        if (exitCodeRpm == 0) {
            val (exitCode, _) = executeCommand("sudo rpm -e $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val packageName = task.get("package_name")?.asString ?: return ""

        // Отримуємо версію deb-пакета
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        if (exitCodeDpkg == 0) {
            val (exitCode, output) = executeCommand("dpkg -s $packageName | grep Version")
            if (exitCode == 0) {
                return output.trim().replace("Version: ", "")
            }
        }

        // Отримуємо версію rpm-пакета
        val (exitCodeRpm, _) = executeCommand("which rpm")
        if (exitCodeRpm == 0) {
            val (exitCode, output) = executeCommand("rpm -q $packageName --qf '%{VERSION}'")
            if (exitCode == 0) {
                return output.trim()
            }
        }

        return ""
    }

    override suspend fun getAvailableVersion(task: JsonObject): String {
        // Логіка для отримання доступної версії пакета
        // Це залежить від джерела пакета
        return ""
    }
}

/**
 * Тип встановлення для звичайних пакетів
 */
class PackageInstallType : BaseInstallType() {
    override val typeName = "package"

    override suspend fun isSupported(): Boolean = true

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val binaryName = task.get("binary_name")?.asString ?: return false
        val (exitCode, _) = executeCommand("which $binaryName")
        return exitCode == 0
    }

    override suspend fun install(task: JsonObject): Boolean {
        // Логіка для встановлення пакета
        return false
    }

    override suspend fun update(task: JsonObject): Boolean {
        return install(task)
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        // Логіка для видалення пакета
        return false
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val binaryName = task.get("binary_name")?.asString ?: return ""
        val versionCmd = task.get("version_cmd")?.asString ?: "--version"

        val (exitCode, output) = executeCommand("$binaryName $versionCmd")
        return if (exitCode == 0) {
            output.trim()
        } else {
            ""
        }
    }

    override suspend fun getAvailableVersion(task: JsonObject): String {
        // Логіка для отримання доступної версії пакета
        return ""
    }
}

/**
 * Тип встановлення через віддалений shell-скрипт
 */
class RemoteShellScriptInstallType : BaseInstallType() {
    override val typeName = "remote_shell_script"

    override suspend fun isSupported(): Boolean = true

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val binaryName = task.get("binary_name")?.asString ?: return false
        val (exitCode, _) = executeCommand("which $binaryName")
        return exitCode == 0
    }

    override suspend fun install(task: JsonObject): Boolean {
        val scriptUrl = task.get("script_url")?.asString ?: return false

        // Завантажуємо скрипт у тимчасовий файл
        val tempScript = "/tmp/install_${System.currentTimeMillis()}.sh"
        if (!downloadFile(scriptUrl, tempScript)) {
            return false
        }

        // Виконуємо скрипт
        val (exitCode, _) = executeCommand("chmod +x $tempScript && $tempScript")

        // Видаляємо тимчасовий файл
        withContext(Dispatchers.IO) {
            File(tempScript).delete()
        }

        return exitCode == 0
    }

    override suspend fun update(task: JsonObject): Boolean {
        return install(task)
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        val uninstallCmd = task.get("uninstall_cmd")?.asString ?: return false

        val (exitCode, _) = executeCommand(uninstallCmd)
        return exitCode == 0
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val binaryName = task.get("binary_name")?.asString ?: return ""
        val versionCmd = task.get("version_cmd")?.asString ?: "--version"

        val (exitCode, output) = executeCommand("$binaryName $versionCmd")
        return if (exitCode == 0) {
            output.trim()
        } else {
            ""
        }
    }

    override suspend fun getAvailableVersion(task: JsonObject): String {
        // Для remote_shell_script складно визначити доступну версію без додаткової інформації
        return ""
    }
}

/**
 * Тип встановлення через команду оболонки
 */
class ShellCmdInstallType : BaseInstallType() {
    override val typeName = "shell_cmd"

    override suspend fun isSupported(): Boolean = true

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val binaryName = task.get("binary_name")?.asString ?: return false
        val (exitCode, _) = executeCommand("which $binaryName")
        return exitCode == 0
    }

    override suspend fun install(task: JsonObject): Boolean {
        val installCmd = task.get("install_cmd")?.asString ?: return false

        val (exitCode, _) = executeCommand(installCmd)
        return exitCode == 0
    }

    override suspend fun update(task: JsonObject): Boolean {
        val updateCmd = task.get("update_cmd")?.asString ?: return install(task)

        val (exitCode, _) = executeCommand(updateCmd)
        return exitCode == 0
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        val uninstallCmd = task.get("uninstall_cmd")?.asString ?: return false

        val (exitCode, _) = executeCommand(uninstallCmd)
        return exitCode == 0
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val binaryName = task.get("binary_name")?.asString ?: return ""
        val versionCmd = task.get("version_cmd")?.asString ?: "--version"

        val (exitCode, output) = executeCommand("$binaryName $versionCmd")
        return if (exitCode == 0) {
            output.trim()
        } else {
            ""
        }
    }

    override suspend fun getAvailableVersion(task: JsonObject): String {
        // Для shell_cmd складно визначити доступну версію без додаткової інформації
        return ""
    }
}

/**
 * Тип встановлення через системний пакетний менеджер
 */
class PackageManagerInstallType : BaseInstallType() {
    override val typeName = "package_manager"

    override suspend fun isSupported(): Boolean {
        // Перевіряємо наявність хоча б одного пакетного менеджера
        val (exitCodeApt, _) = executeCommand("which apt")
        val (exitCodeYum, _) = executeCommand("which yum")
        val (exitCodeDnf, _) = executeCommand("which dnf")
        val (exitCodePacman, _) = executeCommand("which pacman")
        val (exitCodeZypper, _) = executeCommand("which zypper")

        return exitCodeApt == 0 || exitCodeYum == 0 || exitCodeDnf == 0 ||
                exitCodePacman == 0 || exitCodeZypper == 0
    }

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Перевіряємо наявність пакета через доступний пакетний менеджер
        if (isAptAvailable()) {
            val (exitCode, _) = executeCommand("dpkg -s $packageName")
            if (exitCode == 0) return true
        }

        if (isYumAvailable()) {
            val (exitCode, _) = executeCommand("rpm -q $packageName")
            if (exitCode == 0) return true
        }

        if (isPacmanAvailable()) {
            val (exitCode, _) = executeCommand("pacman -Q $packageName")
            if (exitCode == 0) return true
        }

        if (isZypperAvailable()) {
            val (exitCode, _) = executeCommand("rpm -q $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun install(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Встановлюємо пакет через доступний пакетний менеджер
        if (isAptAvailable()) {
            val (exitCode, _) = executeCommand("sudo apt-get install -y $packageName")
            if (exitCode == 0) return true
        }

        if (isYumAvailable()) {
            val (exitCode, _) = executeCommand("sudo yum install -y $packageName")
            if (exitCode == 0) return true
        }

        if (isDnfAvailable()) {
            val (exitCode, _) = executeCommand("sudo dnf install -y $packageName")
            if (exitCode == 0) return true
        }

        if (isPacmanAvailable()) {
            val (exitCode, _) = executeCommand("sudo pacman -S --noconfirm $packageName")
            if (exitCode == 0) return true
        }

        if (isZypperAvailable()) {
            val (exitCode, _) = executeCommand("sudo zypper install -y $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun update(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Оновлюємо пакет через доступний пакетний менеджер
        if (isAptAvailable()) {
            val (exitCode, _) = executeCommand("sudo apt-get upgrade -y $packageName")
            if (exitCode == 0) return true
        }

        if (isYumAvailable()) {
            val (exitCode, _) = executeCommand("sudo yum update -y $packageName")
            if (exitCode == 0) return true
        }

        if (isDnfAvailable()) {
            val (exitCode, _) = executeCommand("sudo dnf update -y $packageName")
            if (exitCode == 0) return true
        }

        if (isPacmanAvailable()) {
            val (exitCode, _) = executeCommand("sudo pacman -Syu --noconfirm $packageName")
            if (exitCode == 0) return true
        }

        if (isZypperAvailable()) {
            val (exitCode, _) = executeCommand("sudo zypper update -y $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Видаляємо пакет через доступний пакетний менеджер
        if (isAptAvailable()) {
            val (exitCode, _) = executeCommand("sudo apt-get remove -y $packageName")
            if (exitCode == 0) return true
        }

        if (isYumAvailable()) {
            val (exitCode, _) = executeCommand("sudo yum remove -y $packageName")
            if (exitCode == 0) return true
        }

        if (isDnfAvailable()) {
            val (exitCode, _) = executeCommand("sudo dnf remove -y $packageName")
            if (exitCode == 0) return true
        }

        if (isPacmanAvailable()) {
            val (exitCode, _) = executeCommand("sudo pacman -R --noconfirm $packageName")
            if (exitCode == 0) return true
        }

        if (isZypperAvailable()) {
            val (exitCode, _) = executeCommand("sudo zypper remove -y $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val packageName = task.get("package_name")?.asString ?: return ""

        // Отримуємо версію пакета через доступний пакетний менеджер
        if (isAptAvailable()) {
            val (exitCode, output) = executeCommand("dpkg -s $packageName | grep Version")
            if (exitCode == 0) {
                return output.trim().replace("Version: ", "")
            }
        }

        if (isYumAvailable() || isDnfAvailable()) {
            val (exitCode, output) = executeCommand("rpm -q --qf '%{VERSION}' $packageName")
            if (exitCode == 0) {
                return output.trim()
            }
        }

        if (isPacmanAvailable()) {
            val (exitCode, output) = executeCommand("pacman -Q $packageName")
            if (exitCode == 0) {
                val parts = output.trim().split(" ")
                if (parts.size >= 2) {
                    return parts[1]
                }
            }
        }

        if (isZypperAvailable()) {
            val (exitCode, output) = executeCommand("rpm -q --qf '%{VERSION}' $packageName")
            if (exitCode == 0) {
                return output.trim()
            }
        }

        return ""
    }

    override suspend fun getAvailableVersion(task: JsonObject): String {
        val packageName = task.get("package_name")?.asString ?: return ""

        // Отримуємо доступну версію пакета через доступний пакетний менеджер
        if (isAptAvailable()) {
            val (exitCode, output) = executeCommand("apt-cache policy $packageName | grep Candidate")
            if (exitCode == 0) {
                return output.trim().replace("Candidate: ", "")
            }
        }

        // Для інших пакетних менеджерів потрібно додати специфічну логіку

        return ""
    }

    // Допоміжні методи для перевірки наявності пакетних менеджерів
    private suspend fun isAptAvailable(): Boolean {
        val (exitCode, _) = executeCommand("which apt")
        return exitCode == 0
    }

    private suspend fun isYumAvailable(): Boolean {
        val (exitCode, _) = executeCommand("which yum")
        return exitCode == 0
    }

    private suspend fun isDnfAvailable(): Boolean {
        val (exitCode, _) = executeCommand("which dnf")
        return exitCode == 0
    }

    private suspend fun isPacmanAvailable(): Boolean {
        val (exitCode, _) = executeCommand("which pacman")
        return exitCode == 0
    }

    private suspend fun isZypperAvailable(): Boolean {
        val (exitCode, _) = executeCommand("which zypper")
        return exitCode == 0
    }
}