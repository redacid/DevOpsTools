package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import com.google.gson.GsonBuilder
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rtextarea.FoldIndicatorStyle
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
        textArea.syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JSON
        textArea.isCodeFoldingEnabled = true
        textArea.isEditable = false

        try {
            val theme = Theme.load(javaClass.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"))
            theme.apply(textArea)
        } catch (e: Exception) {
            textArea.background = Color(0xFCF5FD)
            textArea.foreground = Color(0x767278)
            textArea.currentLineHighlightColor = Color(0xffffd7)
            textArea.caretColor = Color(0x767278)
        }

        //textArea.font = Font("JetBrains Mono", Font.PLAIN, 14)
        textArea.font = Font("DejaVu Mono", Font.PLAIN, 14)
        textArea.setLineWrap(!textArea.lineWrap);
        scrollPane.isIconRowHeaderEnabled = true
        scrollPane.gutter.setFoldIndicatorStyle(FoldIndicatorStyle.CLASSIC);
        try {
            scrollPane.gutter.isBookmarkingEnabled = false
        } catch (e: Exception) {
            // I ignore if the property does not exist
        }
    }

    fun createEditor(): JComponent {
        return JPanel(BorderLayout()).apply {
            background = Color(0x1E1E1E)
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
            jsonEditor.setText("JSON Loading error: ${e.message}")
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        SwingPanel(
            modifier = Modifier.fillMaxSize(),
            factory = { jsonEditor.createEditor() }
        )
    }
}