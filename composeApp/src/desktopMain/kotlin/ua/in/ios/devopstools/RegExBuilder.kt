package ua.`in`.ios.devopstools

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.SwingPanel
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rtextarea.RTextScrollPane
import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

data class RegexMatch(
    val value: String,
    val start: Int,
    val end: Int,
    val groups: List<String>
)

enum class RegexFlag(val flag: String, val description: String) {
    IGNORE_CASE("i", "Case insensitive matching"),
    MULTILINE("m", "Multiline mode"),
    DOTALL("s", "Dot matches newlines"),
    UNICODE("u", "Unicode matching"),
    COMMENTS("x", "Extended syntax (ignore whitespace and comments)")
}

class RegexTextEditor(
    private val syntaxStyle: String,
    private val onTextChanged: ((String) -> Unit)? = null
) {
    private val textArea = RSyntaxTextArea()
    private val scrollPane = RTextScrollPane(textArea)

    init {
        setupTextArea()
    }

    private fun setupTextArea() {
        textArea.syntaxEditingStyle = syntaxStyle
        textArea.isCodeFoldingEnabled = false
        textArea.isEditable = true

        try {
            val theme = Theme.load(javaClass.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"))
            theme.apply(textArea)
        } catch (e: Exception) {
            textArea.background = java.awt.Color(0xFCF5FD)
            textArea.foreground = java.awt.Color(0x767278)
            textArea.currentLineHighlightColor = java.awt.Color(0xffffd7)
            textArea.caretColor = java.awt.Color(0x767278)
        }

        textArea.font = Font("DejaVu Sans Mono", Font.PLAIN, 14)
        textArea.setLineWrap(true)
        textArea.wrapStyleWord = true

        scrollPane.isIconRowHeaderEnabled = false
        scrollPane.gutter.isBookmarkingEnabled = false

        if (onTextChanged != null) {
            textArea.document.addDocumentListener(object : DocumentListener {
                override fun insertUpdate(e: DocumentEvent?) {
                    onTextChanged.invoke(textArea.text)
                }

                override fun removeUpdate(e: DocumentEvent?) {
                    onTextChanged.invoke(textArea.text)
                }

                override fun changedUpdate(e: DocumentEvent?) {
                    onTextChanged.invoke(textArea.text)
                }
            })
        }
    }

    fun createComponent(): JComponent {
        return JPanel(BorderLayout()).apply {
            background = java.awt.Color(0x1E1E1E)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    fun setText(text: String) {
        if (textArea.text != text) {
            textArea.text = text
        }
    }

    fun getText(): String = textArea.text
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegexTester(modifier: Modifier = Modifier) {
    var regexPattern by remember { mutableStateOf("") }
    var testText by remember { mutableStateOf("") }
    var selectedFlags by remember { mutableStateOf(setOf<RegexFlag>()) }
    var matches by remember { mutableStateOf(listOf<RegexMatch>()) }
    var errorMessage by remember { mutableStateOf("") }
    var replaceText by remember { mutableStateOf("") }
    var replaceResult by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    // Create text editor
    val testTextEditor = remember {
        RegexTextEditor(
            syntaxStyle = SyntaxConstants.SYNTAX_STYLE_NONE,
            onTextChanged = { text ->
                testText = text
            }
        )
    }

    fun testRegex() {
        if (regexPattern.isEmpty()) {
            matches = emptyList()
            errorMessage = ""
            return
        }

        try {
            var flags = 0
            selectedFlags.forEach { flag ->
                when (flag) {
                    RegexFlag.IGNORE_CASE -> flags = flags or java.util.regex.Pattern.CASE_INSENSITIVE
                    RegexFlag.MULTILINE -> flags = flags or java.util.regex.Pattern.MULTILINE
                    RegexFlag.DOTALL -> flags = flags or java.util.regex.Pattern.DOTALL
                    RegexFlag.UNICODE -> flags = flags or java.util.regex.Pattern.UNICODE_CASE
                    RegexFlag.COMMENTS -> flags = flags or java.util.regex.Pattern.COMMENTS
                }
            }

            val pattern = java.util.regex.Pattern.compile(regexPattern, flags)
            val matcher = pattern.matcher(testText)

            val foundMatches = mutableListOf<RegexMatch>()
            while (matcher.find()) {
                val groups = mutableListOf<String>()
                for (i in 0..matcher.groupCount()) {
                    groups.add(matcher.group(i) ?: "")
                }

                foundMatches.add(
                    RegexMatch(
                        value = matcher.group(),
                        start = matcher.start(),
                        end = matcher.end(),
                        groups = groups
                    )
                )
            }

            matches = foundMatches
            errorMessage = ""

            // Update replace result
            if (replaceText.isNotEmpty()) {
                replaceResult = pattern.matcher(testText).replaceAll(replaceText)
            } else {
                replaceResult = ""
            }

        } catch (e: Exception) {
            errorMessage = "Regex error: ${e.message}"
            matches = emptyList()
            replaceResult = ""
        }
    }

    LaunchedEffect(regexPattern, testText, selectedFlags, replaceText) {
        testRegex()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "RegEx Builder & Tester",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Regex Pattern Input
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Regular Expression Pattern",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                OutlinedTextField(
                    value = regexPattern,
                    onValueChange = { regexPattern = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter your regex pattern here...") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                    singleLine = false,
                    minLines = 2
                )

                // Flags
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Flags",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    RegexFlag.values().forEach { flag ->
                        FilterChip(
                            selected = flag in selectedFlags,
                            onClick = {
                                selectedFlags = if (flag in selectedFlags) {
                                    selectedFlags - flag
                                } else {
                                    selectedFlags + flag
                                }
                            },
                            label = {
                                Text(
                                    flag.flag,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        )
                    }
                }

                // Flag descriptions
                if (selectedFlags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Column {
                        selectedFlags.forEach { flag ->
                            Text(
                                "${flag.flag}: ${flag.description}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error Message
        if (errorMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        ICON_ERROR,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Test Text and Results
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Test Text
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Test Text",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            TextButton(
                                onClick = {
                                    testText = ""
                                    testTextEditor.setText("")
                                }
                            ) {
                                Text("Clear")
                            }
                        }
                    }

                    SwingPanel(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        factory = { testTextEditor.createComponent() }
                    )
                }
            }

            // Results
            Card(
                modifier = Modifier.weight(1f),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Matches (${matches.size})",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                            if (matches.isNotEmpty()) {
                                TextButton(
                                    onClick = {
                                        val matchesText = matches.joinToString("\n") { match ->
                                            "Match: ${match.value} (${match.start}-${match.end})"
                                        }
                                        clipboardManager.setText(AnnotatedString(matchesText))
                                    }
                                ) {
                                    Text("Copy")
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        if (matches.isEmpty() && regexPattern.isNotEmpty() && testText.isNotEmpty()) {
                            Text(
                                "No matches found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            matches.forEachIndexed { index, match ->
                                MatchCard(match = match, index = index)
                                if (index < matches.size - 1) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Replace Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Replace",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                OutlinedTextField(
                    value = replaceText,
                    onValueChange = { replaceText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Replacement text (use \$1, \$2 for groups)") },
                    label = { Text("Replace with") },
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace)
                )

                if (replaceResult.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Result",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Replaced text:",
                                    style = MaterialTheme.typography.labelMedium
                                )
                                TextButton(
                                    onClick = { clipboardManager.setText(AnnotatedString(replaceResult)) }
                                ) {
                                    Text("Copy")
                                }
                            }
                            Text(
                                replaceResult,
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MatchCard(match: RegexMatch, index: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Match #${index + 1}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Position: ${match.start}-${match.end}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                "Value: \"${match.value}\"",
                style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        RoundedCornerShape(4.dp)
                    )
                    .padding(8.dp)
            )

            if (match.groups.size > 1) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Groups:",
                    style = MaterialTheme.typography.labelMedium
                )
                match.groups.forEachIndexed { groupIndex, group ->
                    Text(
                        "  \$${groupIndex}: \"$group\"",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}