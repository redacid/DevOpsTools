package ua.`in`.ios.devopstools

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

object SudoPasswordCache {
    private var cachedPassword: String? = null
    private var cacheTimestamp: Long = 0
    private const val CACHE_TIMEOUT_MS = 300000 // 5 хвилин

    fun getPassword(): String? {
        // Перевіряємо чи пароль в кеші і чи не застарів він
        if (cachedPassword != null && (System.currentTimeMillis() - cacheTimestamp) < CACHE_TIMEOUT_MS) {
            return cachedPassword
        }
        return null
    }

    fun setPassword(password: String) {
        cachedPassword = password
        cacheTimestamp = System.currentTimeMillis()
    }

    fun clearPassword() {
        cachedPassword = null
    }
}

suspend fun executeCommandSudo(command: String, workingDir: String = ""): Boolean {
    return withContext(Dispatchers.IO) {
        try {
            // Check whether the command needs Sudo
            val useSudo = command.trim().startsWith("sudo ")

            if (useSudo) {
                val commandWithoutSudo = command.trim().substringAfter("sudo ").trim()

                // Check available graphic utilities to enter your password
                val hasZenity = executeCommandDirect("which zenity").first == 0
                val hasPkexec = executeCommandDirect("which pkexec").first == 0
                val hasGksudo = executeCommandDirect("which gksudo").first == 0
                val hasKdesu = executeCommandDirect("which kdesu").first == 0

                // First check if there is a cached password
                val cachedPassword = SudoPasswordCache.getPassword()
                if (cachedPassword != null && hasZenity) {
                    // Use cached password
                    logger.i("executeCommandSudo.cachedPassword", "Using cached sudo password")
                    val sudoCmd = arrayOf("/bin/sh", "-c", "echo '$cachedPassword' | sudo -S $commandWithoutSudo")
                    logger.d("executeCommandSudo.cachedPassword", "Executing command: ${
                        sudoCmd.joinToString(" ").replace(cachedPassword, "*****")
                    }")
                    val process = Runtime.getRuntime().exec(
                        sudoCmd, null,
                        if (workingDir.isEmpty()) null else File(workingDir)
                    )

                    val output = process.inputStream.bufferedReader().use { it.readText() }
                    val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
                    val exitCode = process.waitFor()

                    // If the command has been successfully completed True returns
                    if (exitCode == 0) {
                        return@withContext true
                    } else {
                        // If the command is not fulfilled (perhaps the password is no longer valid),
                        // clean the cache and continue with other methods
                        logger.w("executeCommandSudo.cachedPassword", "Cached sudo password failed, clearing cache")
                        SudoPasswordCache.clearPassword()
                    }
                }

                // If there is no cached password or it has not worked, try other methods
                // Let's try to use Zenity to request a password (and cache)
                if (hasZenity) {
                    logger.i("executeCommandSudo.hasZenity", "Using zenity for sudo password")
                    try {
                        // Ask the password via Zenity
                        val passwordCmd = arrayOf("zenity", "--password", "--title=Enter sudo password")
                        val passwordProcess = Runtime.getRuntime().exec(passwordCmd)
                        val password = passwordProcess.inputStream.bufferedReader().use { it.readText() }
                        val passwordExitCode = passwordProcess.waitFor()

                        if (passwordExitCode == 0 && password.isNotEmpty()) {
                            // Keep the password in the cache
                            SudoPasswordCache.setPassword(password)

                            // Use Sudo -s with a password received
                            val sudoCmd = arrayOf("/bin/sh", "-c", "echo '$password' | sudo -S $commandWithoutSudo")
                            val process = Runtime.getRuntime().exec(
                                sudoCmd, null,
                                if (workingDir.isEmpty()) null else File(workingDir)
                            )
                            logger.d("executeCommandSudo.hasZenity", "Executing command: ${
                                sudoCmd.joinToString(" ").replace(password, "*****")
                            }")
                            val output = process.inputStream.bufferedReader().use { it.readText() }
                            val errorOutput = process.errorStream.bufferedReader().use { it.readText() }
                            val exitCode = process.waitFor()

                            // If the command is not done, clean the cache
                            if (exitCode != 0) {
                                SudoPasswordCache.clearPassword()
                            }

                            return@withContext exitCode == 0
                        }
                    } catch (e: Exception) {
                        logger.e("executeCommandSudo.hasZenity", "Error with zenity+sudo: ${e.message}")
                    }
                }

                if (hasPkexec) {
                    logger.i("executeCommandSudo.hasPkexec", "Using pkexec for sudo command")
                    val result = executeCommandDirect("pkexec $commandWithoutSudo", workingDir)
                    if (result.first == 0) {
                        return@withContext true
                    }
                }

                if (hasGksudo) {
                    logger.i("executeCommandSudo.hasGksudo", "Using gksudo for sudo command")
                    val result = executeCommandDirect("gksudo $commandWithoutSudo", workingDir)
                    if (result.first == 0) {
                        return@withContext true
                    }
                }

                if (hasKdesu) {
                    logger.i("executeCommandSudo.hasKdesu", "Using kdesu for sudo command")
                    val result = executeCommandDirect("kdesu $commandWithoutSudo", workingDir)
                    if (result.first == 0) {
                        return@withContext true
                    }
                }

                logger.e("executeCommandSudo", "Failed to execute sudo command - no suitable sudo method available")
                return@withContext false
            } else {
                // If the command does not need Sudo, perform it in the usual way
                val result = executeCommandDirect(command, workingDir)
                return@withContext result.first == 0
            }
        } catch (e: Exception) {
            logger.e("executeCommandSudo", "Error executing sudo command: ${e.message}")
            return@withContext false
        }
    }
}

suspend fun executeCommandDirect(command: String, workingDir: String = ""): Pair<Int, String> {
    return withContext(Dispatchers.IO) {
        try {
            val processBuilder = ProcessBuilder()
            if (workingDir.isNotEmpty()) {
                processBuilder.directory(File(workingDir))
            }

            // Divide the command into arguments
            val args = command.split("\\s+".toRegex())
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
            logger.e("executeCommandDirect", "Error when executing direct command '$command'", e)
            Pair(-1, e.message ?: "Unknown error")
        }
    }
}