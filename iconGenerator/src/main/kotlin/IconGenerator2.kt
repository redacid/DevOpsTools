package com.icongenerator
import java.awt.*
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.lang.Exception
import java.lang.StringBuilder
import javax.imageio.ImageIO
import java.util.Base64

var inputImageFileName = "../devopstools.png"
var outputImageFilePrefix = "../devopstools_"
var outputClassFileName = "../IconsBase64.kt"
var addPackageString = "package ua.`in`.ios.devopstools"

fun resizeImage(sourceImagePath: String, outputPath: String, targetSize: Int) {
    try {
        // Завантажуємо оригінальне зображення
        val sourceFile = File(sourceImagePath)
        val sourceImage = ImageIO.read(sourceFile)

        // Створюємо новий BufferedImage потрібного розміру
        val resizedImage = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)

        // Отримуємо графічний контекст
        val g2d = resizedImage.createGraphics()

        // Встановлюємо налаштування для найвищої якості масштабування
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // Масштабуємо зображення
        g2d.drawImage(sourceImage, 0, 0, targetSize, targetSize, null)
        g2d.dispose()

        // Зберігаємо результат
        File(outputPath).parentFile?.mkdirs()
        ImageIO.write(resizedImage, "PNG", File(outputPath))

        println("Створено зменшену версію ${targetSize}x${targetSize}: $outputPath")
    } catch (e: Exception) {
        println("Помилка при масштабуванні зображення: ${e.message}")
        //e.printStackTrace()
    }
}
fun generateAllIcons() {
    // Шлях до основної іконки
    val mainIconPath = inputImageFileName
    val mainIconSize = 64

    // Додаткові розміри
    val sizes = listOf(16, 32, 64)

    // Тепер створюємо масштабовані версії з основної іконки
    for (size in sizes) {
        val outputPath = "${outputImageFilePrefix}${size}x${size}.png"
        resizeImage(mainIconPath, outputPath, size)
    }

    println("Всі іконки успішно згенеровано!")
}
fun generatePlatformIcons() {
    // Генеруємо основну іконку
    generateAllIcons()
    println("Згенеровано іконки для всіх платформ")
}
fun imageToBase64(imagePath: String): String {
    try {
        val file = File(imagePath)
        val image = ImageIO.read(file)
        val outputStream = ByteArrayOutputStream()
        ImageIO.write(image, "png", outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.getEncoder().encodeToString(imageBytes)
    } catch (e: Exception) {
        //logger.d("","Помилка при конвертації зображення в Base64",e)
        println("Помилка при конвертації зображення в Base64: ${e.message}")
        //e.printStackTrace()
        return ""
    }
}
fun generateIconsBase64Code(generateToFile: Boolean = false, outputFilePath: String = outputClassFileName) {
    // Шлях до основної іконки
    val mainIconPath = inputImageFileName
    val mainIconSize = 64

    val sb = StringBuilder()

    sb.appendLine(addPackageString)
    sb.appendLine("")
    sb.appendLine("object IconsBase64 {")

    // Генеруємо Base64 для основної іконки
    val mainIconBase64 = imageToBase64(mainIconPath)
    sb.appendLine("    /** Base64 основної іконки FULLSIZE. */")
    sb.appendLine("    const val ICON_FULLSIZE = \"$mainIconBase64\"")
    sb.appendLine("")

    // Генеруємо Base64 для різних розмірів
    val sizes = listOf(16, 32, 64)
    for (size in sizes) {
        val iconPath = "${outputImageFilePrefix}${size}x${size}.png"
        if (File(iconPath).exists()) {
            val iconBase64 = imageToBase64(iconPath)
            sb.appendLine("    /** Base64 іконки ${size}x${size}. */")
            sb.appendLine("    const val ICON_$size = \"$iconBase64\"")
            sb.appendLine("")
        }
    }

    // Додаємо допоміжні методи для декодування
    sb.appendLine("    fun decodeToImage(base64: String): java.awt.image.BufferedImage? {")
    sb.appendLine("        return try {")
    sb.appendLine("            val imageBytes = java.util.Base64.getDecoder().decode(base64)")
    sb.appendLine("            val inputStream = java.io.ByteArrayInputStream(imageBytes)")
    sb.appendLine("            javax.imageio.ImageIO.read(inputStream)")
    sb.appendLine("        } catch (e: Exception) {")
    sb.appendLine("            e.printStackTrace()")
    sb.appendLine("            null")
    sb.appendLine("        }")
    sb.appendLine("    }")
    sb.appendLine("    ")
    sb.appendLine("    fun getIcon(size: Int): java.awt.image.BufferedImage? {")
    sb.appendLine("        return when (size) {")
    sb.appendLine("            16 -> decodeToImage(ICON_16)")
    sb.appendLine("            32 -> decodeToImage(ICON_32)")
    sb.appendLine("            64 -> decodeToImage(ICON_64)")
    sb.appendLine("            else -> {")
    sb.appendLine("                // Повертаємо найближчий доступний розмір")
    sb.appendLine("                when {")
    sb.appendLine("                    size < 16 -> decodeToImage(ICON_16)")
    sb.appendLine("                    size < 32 -> decodeToImage(ICON_16)")
    sb.appendLine("                    size < 64 -> decodeToImage(ICON_32)")
    sb.appendLine("                    else -> decodeToImage(ICON_32)")
    sb.appendLine("                }")
    sb.appendLine("            }")
    sb.appendLine("        }")
    sb.appendLine("    }")
    sb.appendLine("    ")
    sb.appendLine("    fun setWindowIcon(window: java.awt.Window) {")
    sb.appendLine("        try {")
    sb.appendLine("            // Створюємо список іконок різних розмірів")
    sb.appendLine("            val icons = listOf(16, 32, 64)")
    sb.appendLine("                .mapNotNull { size -> getIcon(size) }")
    sb.appendLine("            ")
    sb.appendLine("            // Встановлюємо іконки для вікна")
    sb.appendLine("            if (icons.isNotEmpty()) {")
    sb.appendLine("                window.iconImages = icons")
    sb.appendLine("            }")
    sb.appendLine("        } catch (e: Exception) {")
    sb.appendLine("            println(\"Помилка при встановленні іконки: \${e.message}\")")
    sb.appendLine("        }")
    sb.appendLine("    }")
    sb.appendLine("}")

    val generatedCode = sb.toString()

    // Виводимо результат у консоль
    println("\n--- Початок згенерованого коду ---")
    println(generatedCode)
    println("--- Кінець згенерованого коду ---\n")

    // Зберігаємо у файл, якщо потрібно
    if (generateToFile) {
        try {
            File(outputFilePath).writeText(generatedCode)
            println("Код успішно збережено у файл: $outputFilePath")
        } catch (e: Exception) {
            println("Помилка при збереженні коду у файл: ${e.message}")
        }
    }
}
fun generateAllIconsWithBase64() {
    // Генеруємо всі іконки
    generateAllIcons()
    // Генеруємо Base64-код
    generateIconsBase64Code(true)
    println("Всі іконки і Base64-код успішно згенеровано!")
}
fun main() {
    generateAllIconsWithBase64()
}