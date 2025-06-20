package ua.`in`.ios.devopstools

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

import devopstools.composeapp.generated.resources.Res


@Composable
@Preview
fun App() {
    val settingsManager = SettingsManager.getInstance()

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