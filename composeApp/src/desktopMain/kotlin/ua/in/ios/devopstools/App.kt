package ua.`in`.ios.devopstools

import androidx.compose.runtime.*
import org.jetbrains.compose.ui.tooling.preview.Preview


val logger = Logger.getInstance()

@Composable
@Preview
fun App() {
    val settingsManager = SettingsManager.getInstance()
    val tasksManager = TasksManager.getInstance()
    val systemInfo = SystemInfo.getInstance()

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