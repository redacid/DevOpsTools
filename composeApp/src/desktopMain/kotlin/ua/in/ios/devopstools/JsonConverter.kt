
package ua.`in`.ios.devopstools

import androidx.compose.foundation.background
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalClipboardManager
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
import java.awt.Color
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
import androidx.compose.ui.platform.LocalDensity


enum class ConversionFormat {
    JSON, JSONL, YAML, XML
}

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
            textArea.background = Color(0xFCF5FD)
            textArea.foreground = Color(0x767278)
            textArea.currentLineHighlightColor = Color(0xffffd7)
            textArea.caretColor = Color(0x767278)
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
            background = Color(0x1E1E1E)
            add(scrollPane, BorderLayout.CENTER)
        }
    }

    fun setText(text: String) {
        if (textArea.text != text) {
            textArea.text = text
            textArea.caretPosition = 0
        }
    }

    // Додаємо функцію для оновлення синтаксису
    fun updateSyntaxStyle(newSyntaxStyle: String) {
        if (syntaxStyle != newSyntaxStyle) {
            syntaxStyle = newSyntaxStyle
            textArea.syntaxEditingStyle = syntaxStyle
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
fun JsonConverter(modifier: Modifier = Modifier) {
    var inputFormat by remember { mutableStateOf(ConversionFormat.JSON) }
    var outputFormat by remember { mutableStateOf(ConversionFormat.YAML) }
    var errorMessage by remember { mutableStateOf("") }
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }

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
                    jsonToYaml(inputText)
                }
                inputFormat == ConversionFormat.JSON && outputFormat == ConversionFormat.XML -> {
                    jsonToXml(inputText)
                }
                inputFormat == ConversionFormat.JSON && outputFormat == ConversionFormat.JSONL -> {
                    jsonToJsonl(inputText)
                }
                inputFormat == ConversionFormat.YAML && outputFormat == ConversionFormat.JSON -> {
                    yamlToJson(inputText)
                }
                inputFormat == ConversionFormat.YAML && outputFormat == ConversionFormat.XML -> {
                    val jsonText = yamlToJson(inputText)
                    jsonToXml(jsonText)
                }
                inputFormat == ConversionFormat.YAML && outputFormat == ConversionFormat.JSONL -> {
                    val jsonText = yamlToJson(inputText)
                    jsonToJsonl(jsonText)
                }
                inputFormat == ConversionFormat.XML && outputFormat == ConversionFormat.JSON -> {
                    xmlToJson(inputText)
                }
                inputFormat == ConversionFormat.XML && outputFormat == ConversionFormat.YAML -> {
                    val jsonText = xmlToJson(inputText)
                    jsonToYaml(jsonText)
                }
                inputFormat == ConversionFormat.XML && outputFormat == ConversionFormat.JSONL -> {
                    val jsonText = xmlToJson(inputText)
                    jsonToJsonl(jsonText)
                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.JSON -> {
                    jsonlToJson(inputText)
                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.YAML -> {
                    jsonlToYaml(inputText)
                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.XML -> {
                    jsonlToXml(inputText)
                }
                else -> inputText // Same format
            }

            outputText = newOutputText
            // Обов'язково оновлюємо текст в редакторі
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

    LaunchedEffect(inputText, inputFormat, outputFormat) {
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

// Conversion functions
private fun jsonToYaml(json: String): String {
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .setLenient()
        .create()
    val jsonObject = JsonParser.parseString(json)
    val map = gson.fromJson(jsonObject, Map::class.java)

    val options = DumperOptions()
    options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    options.isPrettyFlow = true
    options.isProcessComments = true
    options.isAllowUnicode = true
    options.defaultScalarStyle = DumperOptions.ScalarStyle.SINGLE_QUOTED
    val yaml = Yaml(options)

    return yaml.dump(map)
}

private fun jsonToXml(json: String): String {
    val gson = GsonBuilder()
        .setPrettyPrinting()
        .setLenient()
        .create()
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
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))

    return writer.toString()
}

private fun yamlToJson(yaml: String): String {
    val yamlParser = Yaml()
    val obj = yamlParser.load<Any>(yaml)

    val gson = GsonBuilder().setPrettyPrinting().create()
    return gson.toJson(obj)
}

private fun xmlToJson(xml: String): String {
    val factory = DocumentBuilderFactory.newInstance()
    val builder = factory.newDocumentBuilder()
    val doc = builder.parse(InputSource(StringReader(xml)))

    val map = xmlToMap(doc.documentElement)

    val gson = GsonBuilder().setPrettyPrinting().create()
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

// JSON Lines conversion functions
private fun jsonToJsonl(json: String): String {
    val gson = GsonBuilder().create()
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

private fun jsonlToJson(jsonl: String): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
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

private fun jsonlToYaml(jsonl: String): String {
    val gson = GsonBuilder().create()
    val lines = jsonl.trim().lines().filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        return ""
    }

    val options = DumperOptions()
    options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    options.isPrettyFlow = true
    options.isProcessComments = true
    options.isAllowUnicode = true
    val yaml = Yaml(options)

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

private fun jsonlToXml(jsonl: String): String {
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
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")

    val writer = StringWriter()
    transformer.transform(DOMSource(doc), StreamResult(writer))

    return writer.toString()
}