package ua.`in`.ios.devopstools

import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview


val logger = Logger.getInstance()
val tasksManager = TasksManager.getInstance()
val settingsManager = SettingsManager.getInstance()
val systemInfo = SystemInfo.getInstance()

@Composable
@Preview
fun App() {
    val loglevel = settingsManager.getString("settings.log_level")
    when (loglevel) {
        "DEBUG" -> logger.setMinimumLogLevel(LogLevel.DEBUG)
        "INFO" -> logger.setMinimumLogLevel(LogLevel.INFO)
        "WARNING" -> logger.setMinimumLogLevel(LogLevel.WARNING)
        "ERROR" -> logger.setMinimumLogLevel(LogLevel.ERROR)
        else -> logger.setMinimumLogLevel(LogLevel.INFO)
    }

//    // Отримання значення налаштування
//    val installPath = settingsManager.getString("settings.install_path")
//    val tasksUrl = settingsManager.getString("settings.tasks_url")
//
//// Зміна значення
//    settingsManager.setString("settings.install_path", "/opt/bin")
//
//// Отримання всіх налаштувань
//    val allSettings = settingsManager.getSettings()

    NavigationDrawer()
}