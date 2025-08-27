package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.DumperOptions
import java.io.StringWriter
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.StringReader
import javax.xml.parsers.DocumentBuilder
import org.xml.sax.InputSource

enum class ConversionFormat {
    JSON,JSONL, YAML, XML
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonConverter(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var inputFormat by remember { mutableStateOf(ConversionFormat.JSON) }
    var outputFormat by remember { mutableStateOf(ConversionFormat.YAML) }
    var errorMessage by remember { mutableStateOf("") }

    val clipboardManager = LocalClipboardManager.current

    fun convertFormat() {
        try {
            errorMessage = ""

            if (inputText.isEmpty()) {
                outputText = ""
                return
            }

            outputText = when {
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
//                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.YAML -> {
//                    val jsonText = jsonlToJson(inputText)
//                    jsonToYaml(jsonText)
//                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.YAML -> {
                    jsonlToYaml(inputText)
                }
//                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.XML -> {
//                    val jsonText = jsonlToJson(inputText)
//                    jsonToXml(jsonText)
//                }
                inputFormat == ConversionFormat.JSONL && outputFormat == ConversionFormat.XML -> {
                    jsonlToXml(inputText)
                }

                else -> inputText // Same format
            }

        } catch (e: Exception) {
            errorMessage = "Conversion error: ${e.message}"
            outputText = ""
        }
    }

    fun copyToClipboard(text: String) {
        clipboardManager.setText(AnnotatedString(text))
    }

    LaunchedEffect(inputText, inputFormat, outputFormat) {
        convertFormat()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "JSON/YAML/XML Converter",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Format selection
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Input format
            Column(modifier = Modifier.weight(1f)) {
                Text("From:", style = MaterialTheme.typography.labelMedium)
                var inputExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = inputExpanded,
                    onExpandedChange = { inputExpanded = it }
                ) {
                    OutlinedTextField(
                        value = inputFormat.name,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = inputExpanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = inputExpanded,
                        onDismissRequest = { inputExpanded = false }
                    ) {
                        ConversionFormat.values().forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.name) },
                                onClick = {
                                    inputFormat = format
                                    inputExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Conversion arrow
            Icon(
                ICON_RIGHT,
                contentDescription = "Convert",
                modifier = Modifier.padding(top = 16.dp)
            )

            // Output format
            Column(modifier = Modifier.weight(1f)) {
                Text("To:", style = MaterialTheme.typography.labelMedium)
                var outputExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = outputExpanded,
                    onExpandedChange = { outputExpanded = it }
                ) {
                    OutlinedTextField(
                        value = outputFormat.name,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = outputExpanded) }
                    )

                    ExposedDropdownMenu(
                        expanded = outputExpanded,
                        onDismissRequest = { outputExpanded = false }
                    ) {
                        ConversionFormat.values().forEach { format ->
                            DropdownMenuItem(
                                text = { Text(format.name) },
                                onClick = {
                                    outputFormat = format
                                    outputExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error message
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
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Input/Output sections
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Input section
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Input (${inputFormat.name})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = { inputText = "" }
                    ) {
                        Text("Clear")
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(400.dp),
                    placeholder = { Text("Paste your ${inputFormat.name.lowercase()} here...") },
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                )
            }

            // Output section
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Output (${outputFormat.name})",
                        style = MaterialTheme.typography.titleMedium
                    )
                    TextButton(
                        onClick = { copyToClipboard(outputText) },
                        enabled = outputText.isNotEmpty()
                    ) {
                        Text("Copy")
                    }
                }

                SelectionContainer {
                    OutlinedTextField(
                        value = outputText,
                        onValueChange = { },
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(400.dp),
                        placeholder = { Text("Converted ${outputFormat.name.lowercase()} will appear here...") },
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }
        }
    }
}

// Conversion functions
private fun jsonToYaml(json: String): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonObject = JsonParser.parseString(json)
    val map = gson.fromJson(jsonObject, Map::class.java)

    val options = DumperOptions()
    options.defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
    options.isPrettyFlow = true
    val yaml = Yaml(options)

    return yaml.dump(map)
}

private fun jsonToXml(json: String): String {
    val gson = GsonBuilder().setPrettyPrinting().create()
    val jsonElement = JsonParser.parseString(json)

    val doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()

    if (jsonElement.isJsonArray) {
        // Якщо це масив, створюємо root з елементами
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
        // Якщо це об'єкт
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
    val gson = GsonBuilder().create() // Не використовуємо pretty printing для JSONL
    val jsonElement = JsonParser.parseString(json)

    return if (jsonElement.isJsonArray) {
        // Якщо це масив JSON об'єктів, кожен елемент на окремий рядок
        val jsonArray = jsonElement.asJsonArray
        jsonArray.joinToString("\n") { element ->
            gson.toJson(element)
        }
    } else {
        // Якщо це один об'єкт, просто повертаємо його як один рядок
        gson.toJson(jsonElement)
    }
}

private fun jsonlToJson(jsonl: String): String {
    val gson = GsonBuilder().setPrettyPrinting().create()

    // Розділяємо на рядки та парсимо кожен
    val lines = jsonl.trim().lines().filter { it.isNotBlank() }

    if (lines.isEmpty()) {
        return "[]"
    }

    if (lines.size == 1) {
        // Один рядок - повертаємо як pretty printed JSON
        val jsonElement = JsonParser.parseString(lines[0])
        return gson.toJson(jsonElement)
    }

    // Кілька рядків - створюємо масив
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
    val yaml = Yaml(options)

    // Кожен JSONL рядок стає окремим YAML документом, розділеним "---"
    return lines.mapIndexed { index, line ->
        val jsonElement = JsonParser.parseString(line)
        val obj = gson.fromJson(jsonElement, Any::class.java)
        val yamlContent = yaml.dump(obj).trim()

        if (index == 0) {
            yamlContent
        } else {
            "\n---\n\n$yamlContent"
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
