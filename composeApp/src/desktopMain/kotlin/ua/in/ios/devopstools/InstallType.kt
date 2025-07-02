package ua.`in`.ios.devopstools

import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption

interface InstallType {
    val typeName: String

    suspend fun isSupported(): Boolean

    suspend fun isInstalled(task: JsonObject): Boolean

    suspend fun install(task: JsonObject): Boolean

    suspend fun update(task: JsonObject): Boolean

    suspend fun uninstall(task: JsonObject): Boolean

    suspend fun getCurrentVersion(task: JsonObject): String

    suspend fun getAvailableVersion(task: JsonObject): String

    companion object {
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

abstract class BaseInstallType : InstallType {
    protected suspend fun executeCommand(command: String, workingDir: String = ""): Pair<Int, String> {
        return withContext(Dispatchers.IO) {
            try {
                // Перевіряємо, чи команда потребує sudo
                val useSudo = command.trim().startsWith("sudo ")

                if (useSudo) {
                    // Перевіряємо наявні графічні утиліти для введення пароля
                    val hasPkexec = executeCommandDirect("which pkexec").first == 0
                    val hasGksudo = executeCommandDirect("which gksudo").first == 0
                    val hasKdesu = executeCommandDirect("which kdesu").first == 0
                    val hasZenity = executeCommandDirect("which zenity").first == 0

                    val commandWithoutSudo = command.trim().substringAfter("sudo ").trim()

                    // Спробуємо використати pkexec (найбільш універсальний)
                    if (hasPkexec) {
                        logger.i("BaseInstallType.ExecuteCommand", "Using pkexec for sudo command")
                        val result = executeCommandDirect("pkexec $commandWithoutSudo", workingDir)
                        if (result.first == 0) {
                            return@withContext result
                        }
                    }

                    // Якщо pkexec не вдалося, спробуємо gksudo (для GNOME)
                    if (hasGksudo) {
                        logger.i("BaseInstallType.ExecuteCommand", "Using gksudo for sudo command")
                        val result = executeCommandDirect("gksudo $commandWithoutSudo", workingDir)
                        if (result.first == 0) {
                            return@withContext result
                        }
                    }

                    // Якщо gksudo не вдалося, спробуємо kdesu (для KDE)
                    if (hasKdesu) {
                        logger.i("BaseInstallType.ExecuteCommand", "Using kdesu for sudo command")
                        val result = executeCommandDirect("kdesu $commandWithoutSudo", workingDir)
                        if (result.first == 0) {
                            return@withContext result
                        }
                    }

                    // Якщо все вище не вдалося, спробуємо zenity
                    if (hasZenity) {
                        logger.i("BaseInstallType.ExecuteCommand", "Using zenity+sudo -S for sudo command")
                        try {
                            // Запитуємо пароль через zenity
                            val passwordCmd = "zenity --password --title=\"Enter sudo password\""
                            val passwordProcess = Runtime.getRuntime().exec(passwordCmd)
                            val password = passwordProcess.inputStream.bufferedReader().use { it.readText() }
                            val passwordExitCode = passwordProcess.waitFor()

                            if (passwordExitCode == 0 && password.isNotEmpty()) {
                                // Створюємо процес з sudo -S для отримання пароля через stdin
                                val sudoCmd = arrayOf("/bin/sh", "-c", "sudo -S $commandWithoutSudo")
                                val process = Runtime.getRuntime().exec(sudoCmd)

                                // Передаємо пароль у stdin процесу
                                process.outputStream.writer().use { writer ->
                                    writer.write("$password\n")
                                    writer.flush()
                                }

                                val output = process.inputStream.bufferedReader().use { it.readText() }
                                val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
                                val exitCode = process.waitFor()

                                val combinedOutput = if (errorOutput.isNotEmpty()) {
                                    "$output\n$errorOutput"
                                } else {
                                    output
                                }

                                return@withContext Pair(exitCode, combinedOutput.trim())
                            }
                        } catch (e: Exception) {
                            logger.e("BaseInstallType.ExecuteCommand", "Error with zenity+sudo -S: ${e.message}")
                        }
                    }

                    // Якщо всі методи не вдалися, повертаємо помилку
                    logger.e("BaseInstallType.ExecuteCommand", "Failed to execute sudo command - no graphical sudo utility available")
                    return@withContext Pair(-1, "Не вдалося виконати команду sudo. Встановіть pkexec, gksudo або kdesu.")
                } else {
                    // Якщо команда не потребує sudo, виконуємо її звичайним способом
                    return@withContext executeCommandDirect(command, workingDir)
                }
            } catch (e: Exception) {
                logger.e("BaseInstallType.ExecuteCommand", "Error when executing command '$command'", e)
                Pair(-1, e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun executeCommandDirect(command: String, workingDir: String = ""): Pair<Int, String> {
        return withContext(Dispatchers.IO) {
            try {

                val args = command.split("\\s+".toRegex())
                val mainCommand = args[0]
                val paths = System.getenv("PATH").split(":")
                val commandExists = paths.any { path ->
                    File("$path/$mainCommand").exists()
                }


                if (!commandExists) {
                    logger.i("BaseInstallType.executeCommandDirect", "Command '$mainCommand' not found")
                    return@withContext Pair(-1, "Command '$mainCommand' not found")
                }

                val processBuilder = ProcessBuilder()
                if (workingDir.isNotEmpty()) {
                    processBuilder.directory(File(workingDir))
                }

                // Split command to arguments
                //val args = command.split("\\s+".toRegex())
                processBuilder.command(args)

                val process = processBuilder.start()
                val output = process.inputStream.bufferedReader().use { it.readText() }
                val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
                val exitCode = process.waitFor()

                val combinedOutput = if (errorOutput.isNotEmpty()) {
                    "$output\n$errorOutput"
                } else {
                    output
                }

                Pair(exitCode, combinedOutput.trim())
            } catch (e: Exception) {
                logger.e("BaseInstallType.executeCommandDirect", "Error when executing direct command '$command'", e)
                Pair(-1, e.message ?: "Unknown error")
            }
        }
    }

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
                false
            }
        }
    }

    protected fun getInstallPath(task: JsonObject): String {
        val settingsManager = SettingsManager.getInstance()
        val installPath = settingsManager.getString("settings.install_path")

        return installPath.ifEmpty {
            "/usr/local/bin"
        }
    }
}

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
        // val releasesJson = try {
        try {
            URL(releasesUrl).readText()
        } catch (e: Exception) {
            logger.e("GithubInstallType.Install", "Error when getting releases info from '$releasesUrl'", e)
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
                logger.e("GithubInstallType.Uninstall", "Error when deleting file '$binaryPath'", e)
                false
            }
        }
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val binaryName = task.get("binary_name")?.asString ?: return ""
        val versionCmd = task.get("version_cmd")?.asString ?: "--version"

        val (exitCode, _) = executeCommand("which $binaryName")
        return if (exitCode == 0) {
        val (_, output) = executeCommand("$binaryName $versionCmd")
          return output.trim()
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
            logger.e("GithubInstallType.getAvailableVersion", "Error when getting latest release info from '$latestReleaseUrl'", e)
            ""
        }
    }

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

class DistributivePackageInstallType : BaseInstallType() {
    override val typeName = "distributive_package"

    override suspend fun isSupported(): Boolean {
        // Checking whether the system supports deb or rpm.
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        val (exitCodeRpm, _) = executeCommand("which rpm")
        return exitCodeDpkg == 0 || exitCodeRpm == 0
    }

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Checking deb packages using dpkg.
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        if (exitCodeDpkg == 0) {
            val (exitCode, _) = executeCommand("dpkg -s $packageName")
            if (exitCode == 0) return true
        }

        // Checking rpm packages using rpm.
        val (exitCodeRpm, _) = executeCommand("which rpm")
        if (exitCodeRpm == 0) {
            val (exitCode, _) = executeCommand("rpm -q $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun install(task: JsonObject): Boolean {
    // Logic for downloading and installing deb/rpm packages
    // Requires additional implementation
        return false
    }

    override suspend fun update(task: JsonObject): Boolean {
        return install(task)
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        val packageName = task.get("package_name")?.asString ?: return false

        // Removing deb packages using dpkg
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        if (exitCodeDpkg == 0) {
            val (exitCode, _) = executeCommand("sudo dpkg -r $packageName")
            if (exitCode == 0) return true
        }

        // Removing rpm packages using rpm.
        val (exitCodeRpm, _) = executeCommand("which rpm")
        if (exitCodeRpm == 0) {
            val (exitCode, _) = executeCommand("sudo rpm -e $packageName")
            if (exitCode == 0) return true
        }

        return false
    }

    override suspend fun getCurrentVersion(task: JsonObject): String {
        val packageName = task.get("package_name")?.asString ?: return ""

        // Getting the version of the deb package
        val (exitCodeDpkg, _) = executeCommand("which dpkg")
        if (exitCodeDpkg == 0) {
            val (exitCode, output) = executeCommand("dpkg -s $packageName | grep Version")
            if (exitCode == 0) {
                return output.trim().replace("Version: ", "")
            }
        }

        // Getting the version of the rpm package.
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
        // Logic for obtaining the available version of the package
        // This depends on the package source
        return ""
    }
}

class PackageInstallType : BaseInstallType() {
    override val typeName = "package"

    override suspend fun isSupported(): Boolean = true

    override suspend fun isInstalled(task: JsonObject): Boolean {
        val binaryName = task.get("binary_name")?.asString ?: return false
        val (exitCode, _) = executeCommand("which $binaryName")
        return exitCode == 0
    }

    override suspend fun install(task: JsonObject): Boolean {
        // Logic for package installation
        return false
    }

    override suspend fun update(task: JsonObject): Boolean {
        return install(task)
    }

    override suspend fun uninstall(task: JsonObject): Boolean {
        // Logic for package deletion
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
        // Logic for obtaining the available version of the package
        return ""
    }
}

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

        // Loading the script into a temporary file.
        val tempScript = "/tmp/install_${System.currentTimeMillis()}.sh"
        if (!downloadFile(scriptUrl, tempScript)) {
            return false
        }

        val (exitCode, _) = executeCommand("chmod +x $tempScript && $tempScript")

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
        // It is difficult to determine the available version for remote_shell_script without additional information.
        return ""
    }
}

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

class PackageManagerInstallType : BaseInstallType() {
    override val typeName = "package_manager"

    override suspend fun isSupported(): Boolean {
        // Checking for the presence of at least one package manager.
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

        // Check the availability of the package through the accessible package manager.
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

        // Install the package through the available package manager.
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

        // Are updating the package through the available package manager.
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

        // Are removing the package using the available package manager.
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

        // Obtain the package version through the available package manager.
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