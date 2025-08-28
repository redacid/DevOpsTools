package ua.`in`.ios.devopstools

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.fife.ui.rsyntaxtextarea.Theme
import org.fife.ui.rtextarea.RTextScrollPane
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import java.awt.BorderLayout
import java.awt.Font
import java.io.StringWriter
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringReader
import org.xml.sax.InputSource
import androidx.compose.foundation.layout.BoxWithConstraints

enum class ConversionFormat {
    JSON, JSONL, YAML, XML
}

data class FormattingOptions(
    // JSON опції
    val jsonPrettyPrint: Boolean = true,
    val jsonIndentSize: Int = 2,
    val jsonSortKeys: Boolean = false,

    // YAML опції
    val yamlFlowStyle: DumperOptions.FlowStyle = DumperOptions.FlowStyle.BLOCK,
    val yamlPrettyFlow: Boolean = true,
    val yamlScalarStyle: DumperOptions.ScalarStyle = DumperOptions.ScalarStyle.SINGLE_QUOTED,
    val yamlAllowUnicode: Boolean = true,
    val yamlProcessComments: Boolean = true,
    val yamlIndentSize: Int = 2,

    // XML опції
    val xmlIndentSize: Int = 2,
    val xmlOmitXmlDeclaration: Boolean = false,
    val xmlPrettyPrint: Boolean = true,

    // Загальні опції редактора
    val fontSize: Int = 12,
    val fontFamily: String = "DejaVu Sans Mono",
    val lineWrap: Boolean = false,
    val showLineNumbers: Boolean = true,
    val theme: String = "idea"
)

class RSyntaxEditor(
    private var syntaxStyle: String,
    private val isEditable: Boolean = true,
    private val onTextChanged: ((String) -> Unit)? = null
) {
    private val textArea = RSyntaxTextArea()
    private val scrollPane = RTextScrollPane(textArea)

    init {
        setupTextArea()
    }

    private fun setupTextArea() {
        textArea.syntaxEditingStyle = syntaxStyle
        textArea.isCodeFoldingEnabled = true
        textArea.isEditable = isEditable

        try {
            val theme = Theme.load(javaClass.getResourceAsStream("/org/fife/ui/rsyntaxtextarea/themes/idea.xml"))
            theme.apply(textArea)
        } catch (e: Exception) {
            textArea.background = java.awt.Color(0xFCF5FD)
            textArea.foreground = java.awt.Color(0x767278)
            textArea.currentLineHighlightColor = java.awt.Color(0xffffd7)
            textArea.caretColor = java.awt.Color(0x767278)
        }

        textArea.font = Font("DejaVu Sans Mono", Font.PLAIN, 12)
        textArea.setLineWrap(false)

        scrollPane.isIconRowHeaderEnabled = true
        scrollPane.gutter.isBookmarkingEnabled = false

        // Add document listener for text changes
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
            textArea.caretPosition = 0
        }
    }

    fun updateSyntaxStyle(newSyntaxStyle: String) {
        if (syntaxStyle != newSyntaxStyle) {
            syntaxStyle = newSyntaxStyle
            textArea.syntaxEditingStyle = syntaxStyle
        }
    }

    // Додаємо функцію для оновлення налаштувань
    fun updateSettings(options: FormattingOptions) {
        textArea.font = Font(options.fontFamily, Font.PLAIN, options.fontSize)
        textArea.setLineWrap(options.lineWrap)
        scrollPane.isIconRowHeaderEnabled = options.showLineNumbers

        // Apply theme
        try {
            val themeResource = "/org/fife/ui/rsyntaxtextarea/themes/${options.theme}.xml"
            val theme = Theme.load(javaClass.getResourceAsStream(themeResource))
            theme?.apply(textArea)
        } catch (e: Exception) {
            // Keep default theme if loading fails
        }
    }

    fun getText(): String = textArea.text

    fun clear() {
        textArea.text = ""
    }
}

private fun getSyntaxStyle(format: ConversionFormat): String {
    return when (format) {
        ConversionFormat.JSON, ConversionFormat.JSONL -> SyntaxConstants.SYNTAX_STYLE_JSON
        ConversionFormat.YAML -> SyntaxConstants.SYNTAX_STYLE_YAML
        ConversionFormat.XML -> SyntaxConstants.SYNTAX_STYLE_XML
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FormattingSettingsPanel(
    options: FormattingOptions,
    onOptionsChanged: (FormattingOptions) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Заголовок з можливістю згортання
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        ICON_SETTINGS,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Налаштування форматування",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Icon(
                    if (expanded) ICON_UP else ICON_DOWN,
                    contentDescription = if (expanded) "Згорнути" else "Розгорнути",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))

                // Вкладки для різних типів налаштувань
                var selectedTabIndex by remember { mutableStateOf(0) }
                val tabs = listOf("JSON", "YAML", "XML", "Редактор")

                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Контент вкладок
                when (selectedTabIndex) {
                    0 -> JsonSettingsTab(options, onOptionsChanged)
                    1 -> YamlSettingsTab(options, onOptionsChanged)
                    2 -> XmlSettingsTab(options, onOptionsChanged)
                    3 -> EditorSettingsTab(options, onOptionsChanged)
                }
            }
        }
    }
}

@Composable
private fun JsonSettingsTab(
    options: FormattingOptions,
    onOptionsChanged: (FormattingOptions) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Pretty print
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Форматування з відступами", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.jsonPrettyPrint,
                onCheckedChange = { onOptionsChanged(options.copy(jsonPrettyPrint = it)) }
            )
        }

        // Indent size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Розмір відступу", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = options.jsonIndentSize.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let {
                            if (it in 1..8) {
                                onOptionsChanged(options.copy(jsonIndentSize = it))
                            }
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Sort keys
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Сортувати ключі", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.jsonSortKeys,
                onCheckedChange = { onOptionsChanged(options.copy(jsonSortKeys = it)) }
            )
        }
    }
}

@Composable
private fun YamlSettingsTab(
    options: FormattingOptions,
    onOptionsChanged: (FormattingOptions) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Flow style
        Column {
            Text("Стиль потоку", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = options.yamlFlowStyle == DumperOptions.FlowStyle.BLOCK,
                    onClick = { onOptionsChanged(options.copy(yamlFlowStyle = DumperOptions.FlowStyle.BLOCK)) },
                    label = { Text("Block") }
                )
                FilterChip(
                    selected = options.yamlFlowStyle == DumperOptions.FlowStyle.FLOW,
                    onClick = { onOptionsChanged(options.copy(yamlFlowStyle = DumperOptions.FlowStyle.FLOW)) },
                    label = { Text("Flow") }
                )
                FilterChip(
                    selected = options.yamlFlowStyle == DumperOptions.FlowStyle.AUTO,
                    onClick = { onOptionsChanged(options.copy(yamlFlowStyle = DumperOptions.FlowStyle.AUTO)) },
                    label = { Text("Auto") }
                )
            }
        }

        // Scalar style
        Column {
            Text("Стиль скалярних значень", style = MaterialTheme.typography.bodyMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = options.yamlScalarStyle == DumperOptions.ScalarStyle.PLAIN,
                    onClick = { onOptionsChanged(options.copy(yamlScalarStyle = DumperOptions.ScalarStyle.PLAIN)) },
                    label = { Text("Plain") }
                )
                FilterChip(
                    selected = options.yamlScalarStyle == DumperOptions.ScalarStyle.SINGLE_QUOTED,
                    onClick = { onOptionsChanged(options.copy(yamlScalarStyle = DumperOptions.ScalarStyle.SINGLE_QUOTED)) },
                    label = { Text("Single") }
                )
                FilterChip(
                    selected = options.yamlScalarStyle == DumperOptions.ScalarStyle.DOUBLE_QUOTED,
                    onClick = { onOptionsChanged(options.copy(yamlScalarStyle = DumperOptions.ScalarStyle.DOUBLE_QUOTED)) },
                    label = { Text("Double") }
                )
            }
        }

        // Pretty flow
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Красиве форматування", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.yamlPrettyFlow,
                onCheckedChange = { onOptionsChanged(options.copy(yamlPrettyFlow = it)) }
            )
        }

        // Allow unicode
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Підтримка Unicode", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.yamlAllowUnicode,
                onCheckedChange = { onOptionsChanged(options.copy(yamlAllowUnicode = it)) }
            )
        }

        // Process comments
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Обробляти коментарі", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.yamlProcessComments,
                onCheckedChange = { onOptionsChanged(options.copy(yamlProcessComments = it)) }
            )
        }

        // Indent size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Розмір відступу", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = options.yamlIndentSize.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let {
                        if (it in 1..8) {
                            onOptionsChanged(options.copy(yamlIndentSize = it))
                        }
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun XmlSettingsTab(
    options: FormattingOptions,
    onOptionsChanged: (FormattingOptions) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Pretty print
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Форматування з відступами", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.xmlPrettyPrint,
                onCheckedChange = { onOptionsChanged(options.copy(xmlPrettyPrint = it)) }
            )
        }

        // Indent size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Розмір відступу", style = MaterialTheme.typography.bodyMedium)
            OutlinedTextField(
                value = options.xmlIndentSize.toString(),
                onValueChange = { value ->
                    value.toIntOrNull()?.let {
                        if (it in 1..8) {
                            onOptionsChanged(options.copy(xmlIndentSize = it))
                        }
                    }
                },
                modifier = Modifier.width(80.dp),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodySmall
            )
        }

        // Omit XML declaration
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Приховати XML декларацію", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.xmlOmitXmlDeclaration,
                onCheckedChange = { onOptionsChanged(options.copy(xmlOmitXmlDeclaration = it)) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditorSettingsTab(
    options: FormattingOptions,
    onOptionsChanged: (FormattingOptions) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Font size
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Розмір шрифту", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = options.fontSize.toString(),
                    onValueChange = { value ->
                        value.toIntOrNull()?.let {
                            if (it in 8..24) {
                                onOptionsChanged(options.copy(fontSize = it))
                            }
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall
                )
                Text(" px", style = MaterialTheme.typography.bodySmall)
            }
        }

        // Font family
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Сімейство шрифтів", style = MaterialTheme.typography.bodyMedium)
            var fontExpanded by remember { mutableStateOf(false) }
            val fonts = listOf("DejaVu Sans Mono", "Courier New", "Monaco", "Consolas")

            ExposedDropdownMenuBox(
                expanded = fontExpanded,
                onExpandedChange = { fontExpanded = !fontExpanded }
            ) {
                OutlinedTextField(
                    value = options.fontFamily,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .width(150.dp)
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = fontExpanded)
                    },
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(
                    expanded = fontExpanded,
                    onDismissRequest = { fontExpanded = false }
                ) {
                    fonts.forEach { font ->
                        DropdownMenuItem(
                            text = { Text(font) },
                            onClick = {
                                onOptionsChanged(options.copy(fontFamily = font))
                                fontExpanded = false
                            }
                        )
                    }
                }
            }
        }

        // Line wrap
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Перенос рядків", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.lineWrap,
                onCheckedChange = { onOptionsChanged(options.copy(lineWrap = it)) }
            )
        }

        // Show line numbers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Показувати номери рядків", style = MaterialTheme.typography.bodyMedium)
            Switch(
                checked = options.showLineNumbers,
                onCheckedChange = { onOptionsChanged(options.copy(showLineNumbers = it)) }
            )
        }

        // Theme
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Тема", style = MaterialTheme.typography.bodyMedium)
            var themeExpanded by remember { mutableStateOf(false) }
            val themes = listOf("idea", "dark", "eclipse", "vs")

            ExposedDropdownMenuBox(
                expanded = themeExpanded,
                onExpandedChange = { themeExpanded = !themeExpanded }
            ) {
                OutlinedTextField(
                    value = options.theme,
                    onValueChange = { },
                    readOnly = true,
                    modifier = Modifier
                        .width(120.dp)
                        .menuAnchor(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = themeExpanded)
                    },
                    textStyle = MaterialTheme.typography.bodySmall
                )
                ExposedDropdownMenu(
                    expanded = themeExpanded,
                    onDismissRequest = { themeExpanded = false }
                ) {
                    themes.forEach { theme ->
                        DropdownMenuItem(
                            text = { Text(theme.uppercase()) },
                            onClick = {
                                onOptionsChanged(options.copy(theme = theme))
                                themeExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonConverter(modifier: Modifier = Modifier) {
    var inputFormat by remember { mutableStateOf(ConversionFormat.JSON) }
    var outputFormat by remember { mutableStateOf(ConversionFormat.YAML) }
    var errorMessage by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }

    // Додаємо стан для налаштувань
    var formattingOptions by remember { mutableStateOf(FormattingOptions()) }

    val clipboardManager = LocalClipboardManager.current

    // Create editors ONCE and update them
    val inputEditor = remember {
        RSyntaxEditor(
            syntaxStyle = getSyntaxStyle(ConversionFormat.JSON),
            isEditable = true,
            onTextChanged = { text ->
                inputText = text
            }
        )
    }

    val outputEditor = remember {
        RSyntaxEditor(
            syntaxStyle = getSyntaxStyle(ConversionFormat.YAML),
            isEditable = false
        )
    }

    // Update syntax styles when formats change
    LaunchedEffect(inputFormat) {
        inputEditor.updateSyntaxStyle(getSyntaxStyle(inputFormat))
    }

    // Update editor settings when options change
    LaunchedEffect(formattingOptions) {
        inputEditor.updateSettings(formattingOptions)
        outputEditor.updateSettings(formattingOptions)
    }

    fun convertFormat() {
        try {
            errorMessage = ""

            if (inputText.isEmpty()) {
                outputText = ""
                outputEditor.setText("")
                return
            }

            val newOutputText = when {
                inputFormat == ConversionFormat.JSON && outputFormat == ConversionFormat.YAML -> {
                    jsonToYaml(inputText, formattingOptions)
                }
                inputFormat == ConversionFormat.JSON && outputFormat == ConversionFormat.XML -> {
                    jsonToXml(inputText, formattingOptions)
                }
                inputFormat == ConversionFormat.JSON && outputFormat == ConversionFormat.JSONL -> {
                    jsonToJsonl(inputText, formattingOptions)
                }
                inputFormat == ConversionFormat.YAML && outputFormat == ConversionFormat.JSON -> {
                    yamlToJson(inputText, formattingOptions)
                }
                inputFormat == ConversionFormat.YAML && outputFormat == ConversionFormat.XML -> {
                    val jsonText = yamlToJson(inputText, formattingOptions)
                    jsonToXml(jsonText, formattingOptions)
                }
                inputFormat == ConversionFormat.YAML && outputFormat == ConversionFormat.JSONL -> {
                    val jsonText = yamlToJson(inputText, formattingOptions)
                    jsonToJsonl(jsonText, formattingOptions)
                }
                inputFormat == ConversionFormat.XML && outputFormat == ConversionFormat.JSON -> {
                    xmlToJson(inputText, formattingOptions)
                }
                inputFormat == ConversionFormat.XML && outputFormat == ConversionFormat.YAML -> {
                    val jsonText = xmlToJson(inputText, formattingOptions)
                    jsonToYaml(jsonText, formattingOptions)
                }
                inputFormat == ConversionFormat.XML && outputFormat == ConversionFormat.JSONL -> {
                    val jsonText = xmlToJson(inputText, formattingOptions)
                    jsonToJsonl(jsonText, formattingOptions)
                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.JSON -> {
                    jsonlToJson(inputText, formattingOptions)
                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.YAML -> {
                    jsonlToYaml(inputText, formattingOptions)
                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.XML -> {
                    jsonlToXml(inputText, formattingOptions)
                }
                else -> inputText // Same format
            }

            outputText = newOutputText
            outputEditor.setText(newOutputText)

        } catch (e: Exception) {
            errorMessage = "Conversion error: ${e.message}"
            outputText = ""
            outputEditor.setText("")
        }
    }

    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
    }

    LaunchedEffect(outputFormat) {
        outputEditor.updateSyntaxStyle(getSyntaxStyle(outputFormat))
        // Також перетворюємо текст заново при зміні output формату
        if (inputText.isNotEmpty()) {
            convertFormat()
        }
    }

    LaunchedEffect(inputText, inputFormat, outputFormat, formattingOptions) {
        convertFormat()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "JSON/YAML/XML Converter",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Додаємо панель налаштувань
        FormattingSettingsPanel(
            options = formattingOptions,
            onOptionsChanged = { formattingOptions = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        // Control panel - покращений дизайн
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Format selection using improved chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Input format selection
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "From:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Використовуємо LazyRow для кращого вигляду
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ConversionFormat.values().forEach { format ->
                                FilterChip(
                                    selected = inputFormat == format,
                                    onClick = { inputFormat = format },
                                    label = {
                                        Text(
                                            format.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (inputFormat == format) {
                                                androidx.compose.ui.text.font.FontWeight.Bold
                                            } else {
                                                androidx.compose.ui.text.font.FontWeight.Normal
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = inputFormat == format,
                                        borderColor = if (inputFormat == format) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        selectedBorderColor = MaterialTheme.colorScheme.primary,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderWidth = 1.dp,
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }
                    }

                    // Conversion arrow with animation
                    Card(
                        modifier = Modifier
                            .padding(top = 24.dp)
                            .size(48.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                ICON_RIGHT,
                                contentDescription = "Convert",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    // Output format selection
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "To:",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ConversionFormat.values().forEach { format ->
                                FilterChip(
                                    selected = outputFormat == format,
                                    onClick = { outputFormat = format },
                                    label = {
                                        Text(
                                            format.name,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (outputFormat == format) {
                                                androidx.compose.ui.text.font.FontWeight.Bold
                                            } else {
                                                androidx.compose.ui.text.font.FontWeight.Normal
                                            }
                                        )
                                    },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        enabled = true,
                                        selected = outputFormat == format,
                                        borderColor = if (outputFormat == format) {
                                            MaterialTheme.colorScheme.secondary
                                        } else {
                                            MaterialTheme.colorScheme.outline
                                        },
                                        selectedBorderColor = MaterialTheme.colorScheme.secondary,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline,
                                        selectedBorderWidth = 1.dp,
                                        borderWidth = 1.dp
                                    )
                                )
                            }
                        }
                    }
                }

                // Error message з кращим дизайном
                if (errorMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                ICON_ERROR,
                                contentDescription = "Error",
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier
                                    .size(20.dp)
                                    .padding(end = 8.dp)
                            )
                            Text(
                                text = errorMessage,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Input/Output sections з можливістю зміни розміру
        ResizablePanels(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            initialSplitRatio = 0.5f,
            leftPanel = { modifier ->
                // Input section
                Card(
                    modifier = modifier,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header для input секції
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        ICON_EDIT,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        "Input (${inputFormat.name})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }

                                OutlinedButton(
                                    onClick = {
                                        inputText = ""
                                        inputEditor.clear()
                                    },
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = MaterialTheme.colorScheme.primary
                                    )
                                ) {
                                    Icon(
                                        ICON_TRASH,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Clear", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Editor area
                        SwingPanel(
                            modifier = Modifier.fillMaxSize(),
                            factory = { inputEditor.createComponent() }
                        )
                    }
                }
            },
            rightPanel = { modifier ->
                // Output section
                Card(
                    modifier = modifier,
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Header для output секції
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            tonalElevation = 2.dp
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        ICON_LOGS,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier
                                            .size(18.dp)
                                            .padding(end = 8.dp)
                                    )
                                    Text(
                                        "Output (${outputFormat.name})",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }

                                Button(
                                    onClick = { copyToClipboard(outputText) },
                                    enabled = outputText.isNotEmpty(),
                                    modifier = Modifier.height(32.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    )
                                ) {
                                    Icon(
                                        ICON_COPY,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Copy", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }

                        // Editor area
                        SwingPanel(
                            modifier = Modifier.fillMaxSize(),
                            factory = { outputEditor.createComponent() }
                        )
                    }
                }
            }
        )
    }
}

@Composable
private fun ResizablePanels(
    modifier: Modifier = Modifier,
    initialSplitRatio: Float = 0.5f,
    leftPanel: @Composable (Modifier) -> Unit,
    rightPanel: @Composable (Modifier) -> Unit
) {
    var splitRatio by remember { mutableFloatStateOf(initialSplitRatio.coerceIn(0.1f, 0.9f)) }
    val density = LocalDensity.current

    BoxWithConstraints(modifier = modifier) {
        val totalWidthPx = with(density) { maxWidth.toPx() }
        val splitterWidth = 8.dp

        Row(modifier = Modifier.fillMaxSize()) {
            // Left panel
            leftPanel(Modifier.weight(splitRatio))

            // Splitter
            Box(
                modifier = Modifier
                    .width(splitterWidth)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { },
                            onDragEnd = { }
                        ) { _, dragAmount ->
                            val deltaRatio = dragAmount.x / totalWidthPx
                            val newRatio = (splitRatio + deltaRatio).coerceIn(0.1f, 0.9f)
                            splitRatio = newRatio
                        }
                    }
                    .hoverable(remember { MutableInteractionSource() })
                    .background(
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(4.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // Visual indicator for splitter
                Box(
                    modifier = Modifier
                        .size(2.dp, 40.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }

            // Right panel
            rightPanel(Modifier.weight(1f - splitRatio))
        }
    }
}

// Conversion functions з підтримкою налаштувань
private fun jsonToYaml(json: String, options: FormattingOptions = FormattingOptions()): String {
    val gsonBuilder = GsonBuilder()
    if (options.jsonPrettyPrint) {
        gsonBuilder.setPrettyPrinting()
    }
    val gson = gsonBuilder.setLenient().create()
    val jsonObject = JsonParser.parseString(json)
    val map = gson.fromJson(jsonObject, Map::class.java)

    val yamlOptions = DumperOptions().apply {
        defaultFlowStyle = options.yamlFlowStyle
        isPrettyFlow = options.yamlPrettyFlow
        isProcessComments = options.yamlProcessComments
        isAllowUnicode = options.yamlAllowUnicode
        defaultScalarStyle = options.yamlScalarStyle
        indent = options.yamlIndentSize
    }
    val yaml = Yaml(yamlOptions)

    return yaml.dump(map)
}

private fun jsonToXml(json: String, options: FormattingOptions = FormattingOptions()): String {
    val gsonBuilder = GsonBuilder()
    if (options.jsonPrettyPrint) {
        gsonBuilder.setPrettyPrinting()
    }
    val gson = gsonBuilder.setLenient().create()
    val jsonElement = JsonParser.parseString(json)

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

    if (jsonElement.isJsonArray) {
        val root = doc.createElement("items")
        doc.appendChild(root)

        val jsonArray = jsonElement.asJsonArray
        jsonArray.forEachIndexed { index, element ->
            val itemElement = doc.createElement("item")
            itemElement.setAttribute("index", index.toString())
            root.appendChild(itemElement)

            when {
                element.isJsonObject -> {
                    val obj = gson.fromJson(element, Map::class.java) as Map<*, *>
                    addMapToXml(doc, itemElement, obj)
                }
                element.isJsonPrimitive -> {
                    itemElement.textContent = element.asString
                }
            }
        }
    } else {
        val root = doc.createElement("root")
        doc.appendChild(root)

        val map = gson.fromJson(jsonElement, Map::class.java) as Map<*, *>
        addMapToXml(doc, root, map)
    }

    val transformer = TransformerFactory.newInstance().newTransformer()
    if (options.xmlPrettyPrint) {
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", options.xmlIndentSize.toString())
    }
    if (options.xmlOmitXmlDeclaration) {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    }

    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))

    return writer.toString()
}

private fun yamlToJson(yaml: String, options: FormattingOptions = FormattingOptions()): String {
    val yamlParser = Yaml()
    val obj = yamlParser.load<Any>(yaml)

    val gsonBuilder = GsonBuilder()
    if (options.jsonPrettyPrint) {
        gsonBuilder.setPrettyPrinting()
    }
    val gson = gsonBuilder.create()
    return gson.toJson(obj)
}

private fun xmlToJson(xml: String, options: FormattingOptions = FormattingOptions()): String {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(InputSource(StringReader(xml)))

    val map = xmlToMap(doc.documentElement)

    val gsonBuilder = GsonBuilder()
    if (options.jsonPrettyPrint) {
        gsonBuilder.setPrettyPrinting()
    }
    val gson = gsonBuilder.create()
    return gson.toJson(map)
}

private fun addMapToXml(doc: Document, parent: Element, map: Map<*, *>) {
    for ((key, value) in map) {
        val element = doc.createElement(key.toString())
        parent.appendChild(element)

        when (value) {
            is Map<*, *> -> addMapToXml(doc, element, value)
            is List<*> -> {
                for (item in value) {
                    val itemElement = doc.createElement("item")
                    element.appendChild(itemElement)
                    when (item) {
                        is Map<*, *> -> addMapToXml(doc, itemElement, item)
                        else -> itemElement.textContent = item.toString()
                    }
                }
            }
            else -> element.textContent = value.toString()
        }
    }
}

private fun xmlToMap(element: Element): Map<String, Any> {
    val map = mutableMapOf<String, Any>()

    val children = element.childNodes
    val childElements = mutableListOf<Element>()

    for (i in 0 until children.length) {
        val node = children.item(i)
        if (node is Element) {
            childElements.add(node)
        }
    }

    if (childElements.isEmpty()) {
        return mapOf(element.tagName to element.textContent)
    }

    val grouped = childElements.groupBy { it.tagName }

    for ((tagName, elements) in grouped) {
        if (elements.size == 1) {
            val child = elements[0]
            val childMap = xmlToMap(child)
            if (childMap.size == 1 && childMap.containsKey(tagName)) {
                map[tagName] = childMap[tagName]!!
            } else {
                map[tagName] = childMap
            }
        } else {
            map[tagName] = elements.map { xmlToMap(it) }
        }
    }

    return map
}

// JSON Lines conversion functions з підтримкою опцій
private fun jsonToJsonl(json: String, options: FormattingOptions = FormattingOptions()): String {
    val gsonBuilder = GsonBuilder()
    val gson = gsonBuilder.create()
    val jsonElement = JsonParser.parseString(json)

    return if (jsonElement.isJsonArray) {
        val jsonArray = jsonElement.asJsonArray
        jsonArray.joinToString("\n") { element ->
            gson.toJson(element)
        }
    } else {
        gson.toJson(jsonElement)
    }
}

private fun jsonlToJson(jsonl: String, options: FormattingOptions = FormattingOptions()): String {
    val gsonBuilder = GsonBuilder()
    if (options.jsonPrettyPrint) {
        gsonBuilder.setPrettyPrinting()
    }
    val gson = gsonBuilder.create()
    val lines = jsonl.trim().lines().filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        return "[]"
    }

    if (lines.size == 1) {
        val jsonElement = JsonParser.parseString(lines[0])
        return gson.toJson(jsonElement)
    }

    val jsonElements = lines.map { line ->
        JsonParser.parseString(line)
    }

    return gson.toJson(jsonElements)
}

private fun jsonlToYaml(jsonl: String, options: FormattingOptions = FormattingOptions()): String {
    val gson = GsonBuilder().create()
    val lines = jsonl.trim().lines().filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        return ""
    }

    val yamlOptions = DumperOptions().apply {
        defaultFlowStyle = options.yamlFlowStyle
        isPrettyFlow = options.yamlPrettyFlow
        isProcessComments = options.yamlProcessComments
        isAllowUnicode = options.yamlAllowUnicode
        indent = options.yamlIndentSize
    }
    val yaml = Yaml(yamlOptions)

    return lines.mapIndexed { index, line ->
        val jsonElement = JsonParser.parseString(line)
        val obj = gson.fromJson(jsonElement, Any::class.java)
        val yamlContent = yaml.dump(obj).trim()

        if (index == 0) {
            yamlContent
        } else {
            "\n---\n$yamlContent"
        }
    }.joinToString("\n")
}

private fun jsonlToXml(jsonl: String, options: FormattingOptions = FormattingOptions()): String {
    val gson = GsonBuilder().create()
    val lines = jsonl.trim().lines().filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        return "<documents></documents>"
    }

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    val root = doc.createElement("documents")
    doc.appendChild(root)

    lines.forEachIndexed { index, line ->
        val jsonElement = JsonParser.parseString(line)
        val obj = gson.fromJson(jsonElement, Map::class.java) as Map<*, *>

        val itemElement = doc.createElement("document")
        itemElement.setAttribute("index", index.toString())
        root.appendChild(itemElement)

        addMapToXml(doc, itemElement, obj)
    }

    val transformer = TransformerFactory.newInstance().newTransformer()
    if (options.xmlPrettyPrint) {
        transformer.setOutputProperty(OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", options.xmlIndentSize.toString())
    }
    if (options.xmlOmitXmlDeclaration) {
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
    }

    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))

    return writer.toString()
}