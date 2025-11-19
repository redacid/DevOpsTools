package ua.`in`.ios.devopstools

import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState

fun main() = application {

    val windowState = rememberWindowState(
        width = 1280.dp,
        height = 864.dp,
        position = WindowPosition(
            x = 100.dp,
            y = 100.dp
        )
    )

    val iconPainter = IconsBase64.getIcon(64)?.let {
        BitmapPainter(it.toComposeImageBitmap())
    }

    Window(
        onCloseRequest = ::exitApplication,
        icon = iconPainter,
        state = windowState,
        title = "DevOpsTools ${System.getProperty("buildVersion") ?: "dev"}",
    ) {
        App(windowState = windowState)
    }
}