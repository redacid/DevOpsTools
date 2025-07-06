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

val imageSizes = listOf(16, 32, 64)

fun resizeImage(sourceImagePath: String, outputPath: String, targetSize: Int) {
    try {
        // Load the original image
        val sourceFile = File(sourceImagePath)
        val sourceImage = ImageIO.read(sourceFile)

        // Create a new Buffiredimage of the right size
        val resizedImage = BufferedImage(targetSize, targetSize, BufferedImage.TYPE_INT_ARGB)

        // Get a graphic context
        val g2d = resizedImage.createGraphics()

        // Set the settings for the highest scaling quality
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

        // We scale the image
        g2d.drawImage(sourceImage, 0, 0, targetSize, targetSize, null)
        g2d.dispose()

        // Зберігаємо результат
        File(outputPath).parentFile?.mkdirs()
        ImageIO.write(resizedImage, "PNG", File(outputPath))

        println("Reduce version ${targetSize}x${targetSize}: $outputPath")
    } catch (e: Exception) {
        println("Error when scalating the image: ${e.message}")
        e.printStackTrace()
    }
}
fun generateAllIcons() {
    // Шлях до основної іконки
    val mainIconPath = inputImageFileName
    val mainIconSize = 64

    for (size in imageSizes) {
        val outputPath = "${outputImageFilePrefix}${size}x${size}.png"
        resizeImage(mainIconPath, outputPath, size)
    }

    println("All icons are successfully generated!")
}
fun generatePlatformIcons() {
    generateAllIcons()
    println("Icons generated")
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
        println("Error when converting an image to Base64: ${e.message}")
        e.printStackTrace()
        return ""
    }
}
fun generateIconsBase64Code(generateToFile: Boolean = false, outputFilePath: String = outputClassFileName) {
    val mainIconPath = inputImageFileName
    val mainIconSize = 64

    val sb = StringBuilder()

    sb.appendLine(addPackageString)
    sb.appendLine("")
    sb.appendLine("object IconsBase64 {")

    val mainIconBase64 = imageToBase64(mainIconPath)
    sb.appendLine("    /** Base64 FULLSIZE icon */")
    sb.appendLine("    const val ICON_FULLSIZE = \"$mainIconBase64\"")
    sb.appendLine("")

    for (size in imageSizes) {
        val iconPath = "${outputImageFilePrefix}${size}x${size}.png"
        if (File(iconPath).exists()) {
            val iconBase64 = imageToBase64(iconPath)
            sb.appendLine("    /** Base64 icon ${size}x${size}. */")
            sb.appendLine("    const val ICON_$size = \"$iconBase64\"")
            sb.appendLine("")
        }
    }

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

    println("\n--- Start of the generated code ---")
    println(generatedCode)
    println("--- End of generated code ---\n")

    if (generateToFile) {
        try {
            File(outputFilePath).writeText(generatedCode)
            println("The code has been successfully saved to the file.: $outputFilePath")
        } catch (e: Exception) {
            println("Error saving the code to the file.: ${e.message}")
        }
    }
}
fun generateAllIconsWithBase64() {
    generateAllIcons()
    generateIconsBase64Code(true)
    println("All icons and Base64-code is successfully generated!")
}
fun main() {
    generateAllIconsWithBase64()
}