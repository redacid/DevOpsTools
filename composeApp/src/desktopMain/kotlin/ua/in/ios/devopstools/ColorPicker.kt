package ua.`in`.ios.devopstools

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.delay
import java.awt.Robot
import java.awt.Toolkit
import kotlin.math.*

data class ColorInfo(
    val color: Color,
    val hex: String,
    val rgb: String,
    val hsl: String,
    val hsv: String,
    val cmyk: String
)

// Color blindness types
enum class ColorBlindnessType {
    NORMAL,
    PROTANOPIA,    // Red-blind
    DEUTERANOPIA,  // Green-blind
    TRITANOPIA,    // Blue-blind
    PROTANOMALY,   // Red-weak
    DEUTERANOMALY, // Green-weak
    TRITANOMALY,   // Blue-weak
    ACHROMATOPSIA  // Complete color blindness
}

// Enhanced screen capture with better stability
private class GlobalScreenCapture {
    private val robot = Robot()

    init {
        // Disable auto delay for faster response
        robot.autoDelay = 0
        //robot.isAutoRepeatOn = false
    }

    fun getPixelColor(x: Int, y: Int): Color {
        return try {
            val awtColor = robot.getPixelColor(x, y)
            Color(awtColor.red, awtColor.green, awtColor.blue)
        } catch (e: Exception) {
            // Return black color as fallback
            Color.Black
        }
    }

    fun getCurrentMousePosition(): Pair<Int, Int> {
        return try {
            val mouseInfo = java.awt.MouseInfo.getPointerInfo()
            Pair(mouseInfo.location.x, mouseInfo.location.y)
        } catch (e: Exception) {
            Pair(0, 0)
        }
    }

    fun getScreenDimensions(): Pair<Int, Int> {
        val screenSize = Toolkit.getDefaultToolkit().screenSize
        return Pair(screenSize.width, screenSize.height)
    }
}

// Function for more precise color capture without crosshair interference
private fun captureColorAtPosition(screenCapture: GlobalScreenCapture): Color {
    return try {
        // Get exact mouse position without any offset
        val (x, y) = screenCapture.getCurrentMousePosition()

        // Capture the pixel color directly at mouse position
        screenCapture.getPixelColor(x, y)
    } catch (e: Exception) {
        Color.Red // Fallback color
    }
}


// Improved global eyedropper overlay
@Composable
private fun GlobalEyedropperOverlay(
    isActive: Boolean,
    onColorPicked: (Color) -> Unit,
    onCancel: () -> Unit
) {
    if (isActive) {
        val screenCapture = remember { GlobalScreenCapture() }
        val (screenWidth, screenHeight) = remember { screenCapture.getScreenDimensions() }

        // Stable color preview - only update when needed
        var currentColor by remember { mutableStateOf(Color.Red) }
        var mousePosition by remember { mutableStateOf(Pair(0, 0)) }
        var isUpdating by remember { mutableStateOf(false) }

        // More stable color tracking with throttling
        LaunchedEffect(isActive) {
            var lastUpdateTime = 0L
            while (isActive && !isUpdating) {
                try {
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastUpdateTime > 100) { // Update every 100ms
                        val (x, y) = screenCapture.getCurrentMousePosition()
                        if (mousePosition != Pair(x, y)) {
                            mousePosition = Pair(x, y)
                            currentColor = screenCapture.getPixelColor(x, y)
                            lastUpdateTime = currentTime
                        }
                    }
                    delay(50)
                } catch (e: Exception) {
                    // Continue silently on errors
                }
            }
        }

        Window(
            onCloseRequest = onCancel,
            state = rememberWindowState(
                position = WindowPosition(0.dp, 0.dp),
                width = screenWidth.dp,
                height = screenHeight.dp
            ),
            title = "Color Picker",
            undecorated = true,
            transparent = true,
            alwaysOnTop = true,
            resizable = false,
            focusable = true
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Transparent)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyDown && keyEvent.key == Key.Escape) {
                            onCancel()
                            true
                        } else {
                            false
                        }
                    }
                    .focusable(true)
                    .pointerInput(Unit) {
                        detectTapGestures { _ ->
                            isUpdating = true // Stop the update loop
                            try {
                                // More precise color capture
                                val color = captureColorAtPosition(screenCapture)
                                onColorPicked(color)
                            } catch (e: Exception) {
                                onCancel()
                            }
                        }
                    }
            ) {
                // Only color preview panel - no crosshair interference
                Column(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(20.dp)
                ) {
                    // Instructions
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.9f)
                        )
                    ) {
                        Text(
                            text = "Click to pick color • ESC to cancel",
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Color preview
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Black.copy(alpha = 0.9f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Color swatch
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(currentColor, RoundedCornerShape(6.dp))
                                    .border(
                                        2.dp,
                                        Color.White,
                                        RoundedCornerShape(6.dp)
                                    )
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Color info
                            Column {
                                Text(
                                    text = "HEX: ${currentColor.toHex()}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "RGB: ${currentColor.toRgbString()}",
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = "Mouse: (${mousePosition.first}, ${mousePosition.second})",
                                    color = Color.White.copy(alpha = 0.8f),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPicker(modifier: Modifier = Modifier) {
    var selectedColor by remember { mutableStateOf(Color.Red) }
    var hexInput by remember { mutableStateOf("#FF0000") }
    var rgbR by remember { mutableStateOf("255") }
    var rgbG by remember { mutableStateOf("0") }
    var rgbB by remember { mutableStateOf("0") }
    var cmykC by remember { mutableStateOf("0") }
    var cmykM by remember { mutableStateOf("100") }
    var cmykY by remember { mutableStateOf("100") }
    var cmykK by remember { mutableStateOf("0") }
    var errorMessage by remember { mutableStateOf("") }

    // Eyedropper state
    var isEyedropperActive by remember { mutableStateOf(false) }
    var eyedropperInstructions by remember { mutableStateOf("Click eyedropper button and click anywhere on screen") }

    // HSV sliders state
    var hsvH by remember { mutableFloatStateOf(0f) }
    var hsvS by remember { mutableFloatStateOf(1f) }
    var hsvV by remember { mutableFloatStateOf(1f) }

    // Color blindness simulator
    var colorBlindnessType by remember { mutableStateOf(ColorBlindnessType.NORMAL) }
    var showColorBlindnessSimulator by remember { mutableStateOf(false) }

    val clipboardManager = LocalClipboardManager.current

    fun updateAllFromColor(color: Color) {
        selectedColor = color
        hexInput = color.toHex()
        val rgb = color.toRgb()
        rgbR = rgb.first.toString()
        rgbG = rgb.second.toString()
        rgbB = rgb.third.toString()
        val cmyk = color.toCmyk()
        cmykC = cmyk.first.toString()
        cmykM = cmyk.second.toString()
        cmykY = cmyk.third.toString()
        cmykK = cmyk.fourth.toString()
        val hsv = rgbToHsv(rgb.first, rgb.second, rgb.third)
        hsvH = hsv.first
        hsvS = hsv.second
        hsvV = hsv.third
        errorMessage = ""
    }

    fun updateColorFromHsv() {
        val color = Color.hsv(hsvH, hsvS, hsvV)
        updateAllFromColor(color)
    }

    fun updateColorFromHex(hex: String) {
        try {
            val color = parseHexColor(hex)
            updateAllFromColor(color)
        } catch (e: Exception) {
            errorMessage = "Invalid HEX format"
        }
    }

    fun updateColorFromRgb() {
        try {
            val r = rgbR.toIntOrNull()?.coerceIn(0, 255) ?: 0
            val g = rgbG.toIntOrNull()?.coerceIn(0, 255) ?: 0
            val b = rgbB.toIntOrNull()?.coerceIn(0, 255) ?: 0
            val color = Color(r, g, b)
            updateAllFromColor(color)
        } catch (e: Exception) {
            errorMessage = "Invalid RGB values"
        }
    }

    fun updateColorFromCmyk() {
        try {
            val c = cmykC.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val m = cmykM.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val y = cmykY.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val k = cmykK.toIntOrNull()?.coerceIn(0, 100) ?: 0
            val color = cmykToColor(c, m, y, k)
            updateAllFromColor(color)
        } catch (e: Exception) {
            errorMessage = "Invalid CMYK values"
        }
    }

    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
    }

    val colorInfo = remember(selectedColor) {
        ColorInfo(
            color = selectedColor,
            hex = selectedColor.toHex(),
            rgb = selectedColor.toRgbString(),
            hsl = selectedColor.toHslString(),
            hsv = selectedColor.toHsvString(),
            cmyk = selectedColor.toCmykString()
        )
    }

    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "Color Picker",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // Eyedropper section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEyedropperActive)
                        MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Eyedropper",
                            style = MaterialTheme.typography.titleSmall
                        )

                        Button(
                            onClick = {
                                if (isEyedropperActive) {
                                    isEyedropperActive = false
                                    eyedropperInstructions = "Click eyedropper button and click anywhere on screen"
                                } else {
                                    isEyedropperActive = true
                                    eyedropperInstructions = "Click anywhere on screen to pick color. ESC to cancel."
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isEyedropperActive)
                                    MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(
                                imageVector = if (isEyedropperActive) ICON_CLOSE else ICON_FEATHER,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isEyedropperActive) "Cancel" else "Pick from Screen")
                        }
                    }

                    Text(
                        text = eyedropperInstructions,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Main layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left column
                Column(modifier = Modifier.weight(1f)) {
                    // Color Wheel and SV Picker
                    Card(elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Color Selection",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            // Color Selection Row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {

                                // Hue Ring
                                HueRing(
                                    modifier = Modifier.size(200.dp),
                                    hue = hsvH,
                                    onHueChanged = { newHue ->
                                        hsvH = newHue
                                        updateColorFromHsv()
                                    }
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                // Saturation/Value 2D Picker
                                SaturationValuePicker(
                                    modifier = Modifier.size(200.dp),
                                    hue = hsvH,
                                    saturation = hsvS,
                                    value = hsvV,
                                    onSVChanged = { newS, newV ->
                                        hsvS = newS
                                        hsvV = newV
                                        updateColorFromHsv()
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Current Color Display with Color Blindness Simulation
                    Card {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Current Color",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Normal color
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = selectedColor),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(80.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Normal",
                                            color = if (selectedColor.calculateLuminance() > 0.5) Color.Black else Color.White,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                // Color blind simulation
                                if (showColorBlindnessSimulator) {
                                    val simulatedColor = simulateColorBlindness(selectedColor, colorBlindnessType)
                                    Card(
                                        colors = CardDefaults.cardColors(containerColor = simulatedColor),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(80.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = colorBlindnessType.name.lowercase().replaceFirstChar { it.uppercase() },
                                                color = if (simulatedColor.calculateLuminance() > 0.5) Color.Black else Color.White,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }

                            // Color blindness controls
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Switch(
                                    checked = showColorBlindnessSimulator,
                                    onCheckedChange = { showColorBlindnessSimulator = it }
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Color Blindness Simulator", style = MaterialTheme.typography.bodySmall)
                            }

                            if (showColorBlindnessSimulator) {
                                var expanded by remember { mutableStateOf(false) }

                                ExposedDropdownMenuBox(
                                    expanded = expanded,
                                    onExpandedChange = { expanded = it }
                                ) {
                                    OutlinedTextField(
                                        value = colorBlindnessType.name.lowercase().replaceFirstChar { it.uppercase() },
                                        onValueChange = { },
                                        readOnly = true,
                                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                        label = { Text("Type") }
                                    )

                                    ExposedDropdownMenu(
                                        expanded = expanded,
                                        onDismissRequest = { expanded = false }
                                    ) {
                                        ColorBlindnessType.values().forEach { type ->
                                            DropdownMenuItem(
                                                text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                                onClick = {
                                                    colorBlindnessType = type
                                                    expanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Right column
                Column(modifier = Modifier.weight(1f)) {
                    // HSV Sliders
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "HSV Sliders",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Hue Slider
                            Text("Hue: ${hsvH.toInt()}°", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = hsvH,
                                onValueChange = {
                                    hsvH = it
                                    updateColorFromHsv()
                                },
                                valueRange = 0f..360f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Saturation Slider
                            Text("Saturation: ${(hsvS * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = hsvS,
                                onValueChange = {
                                    hsvS = it
                                    updateColorFromHsv()
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Value Slider
                            Text("Brightness: ${(hsvV * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
                            Slider(
                                value = hsvV,
                                onValueChange = {
                                    hsvV = it
                                    updateColorFromHsv()
                                },
                                valueRange = 0f..1f,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Input Methods
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Color Input",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // HEX Input
                            OutlinedTextField(
                                value = hexInput,
                                onValueChange = {
                                    hexInput = it
                                    updateColorFromHex(it)
                                },
                                label = { Text("HEX") },
                                placeholder = { Text("#FF0000") },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodySmall
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            // RGB Inputs
                            Text(
                                text = "RGB",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                OutlinedTextField(
                                    value = rgbR,
                                    onValueChange = {
                                        rgbR = it
                                        updateColorFromRgb()
                                    },
                                    label = { Text("R") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )

                                OutlinedTextField(
                                    value = rgbG,
                                    onValueChange = {
                                        rgbG = it
                                        updateColorFromRgb()
                                    },
                                    label = { Text("G") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )

                                OutlinedTextField(
                                    value = rgbB,
                                    onValueChange = {
                                        rgbB = it
                                        updateColorFromRgb()
                                    },
                                    label = { Text("B") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    textStyle = MaterialTheme.typography.bodySmall
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // CMYK Inputs
                            Text(
                                text = "CMYK",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                listOf(
                                    "C" to cmykC,
                                    "M" to cmykM,
                                    "Y" to cmykY,
                                    "K" to cmykK
                                ).forEachIndexed { index, (label, value) ->
                                    OutlinedTextField(
                                        value = value,
                                        onValueChange = { newValue ->
                                            when (index) {
                                                0 -> cmykC = newValue
                                                1 -> cmykM = newValue
                                                2 -> cmykY = newValue
                                                3 -> cmykK = newValue
                                            }
                                            updateColorFromCmyk()
                                        },
                                        label = { Text(label) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.weight(1f),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error Display
            if (errorMessage.isNotEmpty()) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Color Information with Copy functionality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Color Information (Click to Copy)",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            ClickableCopyRow("HEX:", colorInfo.hex) { copyToClipboard(it) }
                            ClickableCopyRow("RGB:", colorInfo.rgb) { copyToClipboard(it) }
                            ClickableCopyRow("HSL:", colorInfo.hsl) { copyToClipboard(it) }
                            ClickableCopyRow("HSV:", colorInfo.hsv) { copyToClipboard(it) }
                            ClickableCopyRow("CMYK:", colorInfo.cmyk) { copyToClipboard(it) }
                            ClickableCopyRow("INT:", selectedColor.toArgb().toString()) { copyToClipboard(it) }
                        }
                    }
                }

                // Predefined Colors
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Popular Colors",
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        CompactColorGrid { color ->
                            updateAllFromColor(color)
                        }
                    }
                }
            }
        }

        // Global eyedropper overlay
        GlobalEyedropperOverlay(
            isActive = isEyedropperActive,
            onColorPicked = { color ->
                updateAllFromColor(color)
                isEyedropperActive = false
                eyedropperInstructions = "Color successfully picked: ${color.toHex()}"
            },
            onCancel = {
                isEyedropperActive = false
                eyedropperInstructions = "Color selection cancelled"
            }
        )
    }
}

@Composable
private fun ClickableCopyRow(
    label: String,
    value: String,
    onCopy: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCopy(value) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1.5f)
        )
    }
}

@Composable
private fun HueRing(
    modifier: Modifier = Modifier,
    hue: Float,
    onHueChanged: (Float) -> Unit
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val deltaX = offset.x - center.x
                    val deltaY = offset.y - center.y
                    val angle = atan2(deltaY, deltaX)
                    val newHue: Float = ((angle * 180f / PI + 360f) % 360f).toFloat()
                    onHueChanged(newHue)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val deltaX = change.position.x - center.x
                    val deltaY = change.position.y - center.y
                    val angle = atan2(deltaY, deltaX)
                    val newHue: Float = ((angle * 180f / PI + 360f) % 360f).toFloat()
                    onHueChanged(newHue)
                }
            }
    ) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = minOf(size.width, size.height) / 2f
        val ringWidth = 20f

        // Draw hue ring
        for (angle in 0 until 360) {
            val color = Color.hsv(angle.toFloat(), 1f, 1f)
            val startRadius = radius - ringWidth
            val endRadius = radius

            drawArc(
                color = color,
                startAngle = angle.toFloat() - 0.5f,
                sweepAngle = 1f,
                useCenter = false,
                topLeft = Offset(center.x - endRadius, center.y - endRadius),
                size = Size(endRadius * 2f, endRadius * 2f),
                style = Stroke(width = ringWidth)
            )
        }

        // Draw current hue indicator
        val currentHueAngle = hue * PI / 180f
        val indicatorRadius = radius - ringWidth / 2f
        val indicatorX = center.x + indicatorRadius * cos(currentHueAngle).toFloat()
        val indicatorY = center.y + indicatorRadius * sin(currentHueAngle).toFloat()

        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(indicatorX, indicatorY),
            style = Stroke(width = 3f)
        )
    }
}

@Composable
private fun SaturationValuePicker(
    modifier: Modifier = Modifier,
    hue: Float,
    saturation: Float,
    value: Float,
    onSVChanged: (Float, Float) -> Unit
) {
    Canvas(
        modifier = modifier
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newS = (offset.x / size.width).coerceIn(0f, 1f)
                    val newV = 1f - (offset.y / size.height).coerceIn(0f, 1f)
                    onSVChanged(newS, newV)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val newS = (change.position.x / size.width).coerceIn(0f, 1f)
                    val newV = 1f - (change.position.y / size.height).coerceIn(0f, 1f)
                    onSVChanged(newS, newV)
                }
            }
    ) {
        // Draw saturation-value square
        val steps = 50
        val stepWidth = size.width / steps
        val stepHeight = size.height / steps

        for (x in 0 until steps) {
            for (y in 0 until steps) {
                val s = x.toFloat() / steps
                val v = 1f - (y.toFloat() / steps)
                val color = Color.hsv(hue, s, v)

                drawRect(
                    color = color,
                    topLeft = Offset(x * stepWidth, y * stepHeight),
                    size = Size(stepWidth, stepHeight)
                )
            }
        }

        // Draw current position indicator
        val indicatorX = saturation * size.width
        val indicatorY = (1f - value) * size.height

        drawCircle(
            color = Color.White,
            radius = 8f,
            center = Offset(indicatorX, indicatorY),
            style = Stroke(width = 3f)
        )
        drawCircle(
            color = Color.Black,
            radius = 6f,
            center = Offset(indicatorX, indicatorY),
            style = Stroke(width = 1f)
        )
    }
}

@Composable
private fun CompactColorGrid(onColorSelected: (Color) -> Unit) {
    val colors = listOf(
        Color.Red, Color.Green, Color.Blue, Color.Yellow,
        Color.Cyan, Color.Magenta, Color.Black, Color.White,
        Color.Gray, Color.DarkGray, Color.LightGray, Color.Transparent,
        Color(0xFF1976D2), Color(0xFF388E3C), Color(0xFFD32F2F), Color(0xFFF57C00),
        Color(0xFF7B1FA2), Color(0xFF0097A7), Color(0xFF5D4037), Color(0xFF455A64)
    )

    val chunkedColors = colors.chunked(10)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        chunkedColors.forEach { rowColors ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                rowColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .border(
                                0.5.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(4.dp)
                            )
                            .clickable { onColorSelected(color) }
                    )
                }
            }
        }
    }
}

// Color blindness simulation functions
private fun simulateColorBlindness(color: Color, type: ColorBlindnessType): Color {
    if (type == ColorBlindnessType.NORMAL) return color

    val (r, g, b) = color.toRgb()
    val rF = r / 255f
    val gF = g / 255f
    val bF = b / 255f

    val (newR, newG, newB) = when (type) {
        ColorBlindnessType.PROTANOPIA -> {
            // Red-blind simulation
            Triple(0.567f * rF + 0.433f * gF, 0.558f * rF + 0.442f * gF, 0.242f * gF + 0.758f * bF)
        }
        ColorBlindnessType.DEUTERANOPIA -> {
            // Green-blind simulation
            Triple(0.625f * rF + 0.375f * gF, 0.7f * rF + 0.3f * gF, 0.3f * gF + 0.7f * bF)
        }
        ColorBlindnessType.TRITANOPIA -> {
            // Blue-blind simulation
            Triple(0.95f * rF + 0.05f * gF, 0.433f * gF + 0.567f * bF, 0.475f * gF + 0.525f * bF)
        }
        ColorBlindnessType.PROTANOMALY -> {
            // Red-weak simulation
            Triple(0.817f * rF + 0.183f * gF, 0.333f * rF + 0.667f * gF, 0.125f * gF + 0.875f * bF)
        }
        ColorBlindnessType.DEUTERANOMALY -> {
            // Green-weak simulation
            Triple(0.8f * rF + 0.2f * gF, 0.258f * rF + 0.742f * gF, 0.142f * gF + 0.858f * bF)
        }
        ColorBlindnessType.TRITANOMALY -> {
            // Blue-weak simulation
            Triple(0.967f * rF + 0.033f * gF, 0.733f * gF + 0.267f * bF, 0.183f * gF + 0.817f * bF)
        }
        ColorBlindnessType.ACHROMATOPSIA -> {
            // Complete color blindness (monochrome)
            val gray = 0.299f * rF + 0.587f * gF + 0.114f * bF
            Triple(gray, gray, gray)
        }
        else -> Triple(rF, gF, bF)
    }

    return Color(
        (newR * 255).toInt().coerceIn(0, 255),
        (newG * 255).toInt().coerceIn(0, 255),
        (newB * 255).toInt().coerceIn(0, 255)
    )
}

// Extension function for luminance calculation
private fun Color.calculateLuminance(): Float {
    val (r, g, b) = toRgb()

    // Convert RGB to linear RGB
    fun toLinear(component: Int): Float {
        val c = component / 255f
        return if (c <= 0.03928f) {
            c / 12.92f
        } else {
            ((c + 0.055f) / 1.055f).pow(2.4f)
        }
    }

    val rLinear = toLinear(r)
    val gLinear = toLinear(g)
    val bLinear = toLinear(b)

    // Calculate relative luminance using the ITU-R BT.709 coefficients
    return 0.2126f * rLinear + 0.7152f * gLinear + 0.0722f * bLinear
}

// Extension functions for color conversion
private fun Color.toHex(): String {
    val argb = this.toArgb()
    return "#${String.format("%08X", argb).substring(2)}"
}

private fun Color.toRgb(): Triple<Int, Int, Int> {
    val argb = this.toArgb()
    val r = (argb shr 16) and 0xFF
    val g = (argb shr 8) and 0xFF
    val b = argb and 0xFF
    return Triple(r, g, b)
}

private fun Color.toRgbString(): String {
    val (r, g, b) = toRgb()
    return "rgb($r, $g, $b)"
}

private fun Color.toHslString(): String {
    val (r, g, b) = toRgb()
    val (h, s, l) = rgbToHsl(r, g, b)
    return "hsl(${h.toInt()}°, ${(s * 100).toInt()}%, ${(l * 100).toInt()}%)"
}

private fun Color.toHsvString(): String {
    val (r, g, b) = toRgb()
    val (h, s, v) = rgbToHsv(r, g, b)
    return "hsv(${h.toInt()}°, ${(s * 100).toInt()}%, ${(v * 100).toInt()}%)"
}

private fun parseHexColor(hex: String): Color {
    val cleanHex = hex.removePrefix("#")
    if (cleanHex.length != 6) throw IllegalArgumentException("Invalid hex format")

    val r = cleanHex.substring(0, 2).toInt(16)
    val g = cleanHex.substring(2, 4).toInt(16)
    val b = cleanHex.substring(4, 6).toInt(16)

    return Color(r, g, b)
}

private fun rgbToHsl(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rF = r / 255f
    val gF = g / 255f
    val bF = b / 255f

    val max = maxOf(rF, gF, bF)
    val min = minOf(rF, gF, bF)
    val delta = max - min

    val l = (max + min) / 2f

    if (delta == 0f) {
        return Triple(0f, 0f, l)
    }

    val s = if (l < 0.5f) delta / (max + min) else delta / (2f - max - min)

    val h = when (max) {
        rF -> ((gF - bF) / delta + if (gF < bF) 6f else 0f) * 60f
        gF -> ((bF - rF) / delta + 2f) * 60f
        bF -> ((rF - gF) / delta + 4f) * 60f
        else -> 0f
    }

    return Triple(h, s, l)
}

private fun rgbToHsv(r: Int, g: Int, b: Int): Triple<Float, Float, Float> {
    val rF = r / 255f
    val gF = g / 255f
    val bF = b / 255f

    val max = maxOf(rF, gF, bF)
    val min = minOf(rF, gF, bF)
    val delta = max - min

    val v = max
    val s = if (max == 0f) 0f else delta / max

    val h = if (delta == 0f) 0f else when (max) {
        rF -> ((gF - bF) / delta + if (gF < bF) 6f else 0f) * 60f
        gF -> ((bF - rF) / delta + 2f) * 60f
        bF -> ((rF - gF) / delta + 4f) * 60f
        else -> 0f
    }

    return Triple(h, s, v)
}

// CMYK extension functions
private fun Color.toCmyk(): Quadruple<Int, Int, Int, Int> {
    val (r, g, b) = toRgb()
    val rF = r / 255f
    val gF = g / 255f
    val bF = b / 255f

    val k = 1f - maxOf(rF, gF, bF)

    if (k == 1f) {
        return Quadruple(0, 0, 0, 100)
    }

    val c = ((1f - rF - k) / (1f - k) * 100).toInt()
    val m = ((1f - gF - k) / (1f - k) * 100).toInt()
    val y = ((1f - bF - k) / (1f - k) * 100).toInt()
    val kPercent = (k * 100).toInt()

    return Quadruple(c, m, y, kPercent)
}

private fun Color.toCmykString(): String {
    val (c, m, y, k) = toCmyk()
    return "cmyk($c%, $m%, $y%, $k%)"
}

private fun cmykToColor(c: Int, m: Int, y: Int, k: Int): Color {
    val cF = c / 100f
    val mF = m / 100f
    val yF = y / 100f
    val kF = k / 100f

    val r = ((1f - cF) * (1f - kF) * 255).toInt().coerceIn(0, 255)
    val g = ((1f - mF) * (1f - kF) * 255).toInt().coerceIn(0, 255)
    val b = ((1f - yF) * (1f - kF) * 255).toInt().coerceIn(0, 255)

    return Color(r, g, b)
}

// Helper data class for four values
private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)