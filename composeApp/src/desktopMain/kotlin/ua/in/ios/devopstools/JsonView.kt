package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.google.gson.GsonBuilder
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Font
import java.io.File
import javax.swing.*

class RSyntaxJsonEditor {
    private val textArea = RSyntaxTextArea()
    private val scrollPane = RTextScrollPane(textArea)

    init {
        setupTextArea()
    }

    private fun setupTextArea() {
        // Налаштування синтаксису для JSON
        textArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
        textArea.isCodeFoldingEnabled = true
        textArea.isEditable = false

        // Темна тема
        try {
            val theme = Theme.load(javaClass.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/dark.xml"))
            theme.apply(textArea)
        } catch (e: Exception) {
            // Якщо не вдалося завантажити тему, налаштовуємо вручну
            textArea.background = Color(0x1E1E1E)
            textArea.foreground = Color(0xD4D4D4)
            textArea.currentLineHighlightColor = Color(0x2D2D30)
            textArea.caretColor = Color(0xD4D4D4)
        }

        // Шрифт
        textArea.font = Font("JetBrains Mono", Font.PLAIN, 14)

        // Налаштування скролу
        scrollPane.isIconRowHeaderEnabled = true
        // Вимкнення букмарків якщо потрібно
        try {
            scrollPane.gutter.isBookmarkingEnabled = false
        } catch (e: Exception) {
            // Ігноруємо якщо властивість не існує
        }
    }

    fun createEditor(): JComponent {
        return JPanel(BorderLayout()).apply {
            background = Color(0x1E1E1E)

            val toolbar = JPanel().apply {
                background = Color(0x1E1E1E)

                add(JButton("Згорнути все").apply {
                    background = Color(0x3C3C3C)
                    foreground = Color(0xD4D4D4)
                    border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    addActionListener {
                        // Згортаємо всі блоки
                        val foldManager = textArea.foldManager
                        for (i in 0 until foldManager.foldCount) {
                            val fold = foldManager.getFold(i)
                            fold.isCollapsed = true
                        }
                        textArea.repaint()
                    }
                })

                add(JButton("Розгорнути все").apply {
                    background = Color(0x3C3C3C)
                    foreground = Color(0xD4D4D4)
                    border = BorderFactory.createEmptyBorder(5, 10, 5, 10)
                    addActionListener {
                        // Розгортаємо всі блоки
                        val foldManager = textArea.foldManager
                        for (i in 0 until foldManager.foldCount) {
                            val fold = foldManager.getFold(i)
                            fold.isCollapsed = false
                        }
                        textArea.repaint()
                    }
                })
            }

            add(toolbar, BorderLayout.NORTH)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    fun setText(text: String) {
        textArea.text = text
        textArea.caretPosition = 0
    }
}

@Composable
fun JsonViewer() {
    val tasksManager = TasksManager.getInstance()
    val jsonEditor = remember { RSyntaxJsonEditor() }

    LaunchedEffect(Unit) {
        try {
            val jsonContent = File(tasksManager.tasksFile).readText()
            val gson = GsonBuilder().setPrettyPrinting().create()
            val formattedJson = gson.toJson(gson.fromJson(jsonContent, Any::class.java))
            jsonEditor.setText(formattedJson)
        } catch (e: Exception) {
            jsonEditor.setText("Помилка завантаження JSON: ${e.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = { jsonEditor.createEditor() }
        )
    }
}