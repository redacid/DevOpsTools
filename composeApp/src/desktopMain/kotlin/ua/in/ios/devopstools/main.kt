package ua.`in`.ios.devopstools

import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application

fun main() = application {

    val windowState = remember {
        WindowState(
            width = 1280.dp,
            height = 864.dp,
            position = androidx.compose.ui.window.WindowPosition(
                x = 100.dp,
                y = 100.dp,
            ),
//            isMinimized = false,

//            placement = androidx.compose.ui.window.WindowPlacement.Floating,
//            resizable = true,
//            enabled = true,
//            focusable = true,
        )
    }

    val iconPainter = IconsBase64.getIcon(64)?.let {
        BitmapPainter(it.toComposeImageBitmap())
    }

    Window(
        onCloseRequest = ::exitApplication,
        icon = iconPainter,
        state = windowState,
        title = "DevOpsTools ${System.getProperty("buildVersion") ?: "dev"}",
    ) {
        App()
    }
}