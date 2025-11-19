package ua.`in`.ios.devopstools

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import java.util.regex.PatternSyntaxException

@Composable
fun RegexTester(modifier: Modifier = Modifier) {
    var inputText by remember { mutableStateOf("") }
    var regexPattern by remember { mutableStateOf("") }
    var testResults by remember { mutableStateOf("") }
    var analysisResults by remember { mutableStateOf("") }
    var generatedRegex by remember { mutableStateOf("") }
    var isValidRegex by remember { mutableStateOf(true) }
    var regexError by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "RegEx Generator/Tester",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Input text section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Text for Analysis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("Paste your text here for analysis and regex generation...") },
                    singleLine = false,
                    maxLines = 6
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                analysisResults = analyzeText(inputText)
                                generatedRegex = generateRegexFromText(inputText)
                            }
                        },
                        enabled = inputText.isNotEmpty()
                    ) {
                        Icon(ICON_SEARCH, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Analyze")
                    }

                    Button(
                        onClick = {
                            scope.launch {
                                generatedRegex = generateRegexFromText(inputText)
                            }
                        },
                        enabled = inputText.isNotEmpty()
                    ) {
                        Icon(ICON_CODE, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Generate RegEx")
                    }

                    Button(
                        onClick = {
                            inputText = ""
                            regexPattern = ""
                            testResults = ""
                            analysisResults = ""
                            generatedRegex = ""
                        }
                    ) {
                        Text("Clear")
                    }
                }
            }
        }

        // Regular expression section
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Regular Expression",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = regexPattern,
                    onValueChange = { newValue ->
                        regexPattern = newValue
                        // Real-time regex validation
                        try {
                            Pattern.compile(newValue)
                            isValidRegex = true
                            regexError = ""
                        } catch (e: PatternSyntaxException) {
                            isValidRegex = false
                            regexError = e.description ?: "Invalid syntax"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Enter regular expression or use generated one...") },
                    isError = !isValidRegex,
                    supportingText = {
                        if (!isValidRegex) {
                            Text(
                                text = regexError,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            scope.launch {
                                testResults = testRegex(regexPattern, inputText)
                            }
                        },
                        enabled = regexPattern.isNotEmpty() && inputText.isNotEmpty() && isValidRegex
                    ) {
                        Icon(ICON_CONNECT, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Test")
                    }

                    if (generatedRegex.isNotEmpty()) {
                        Button(
                            onClick = {
                                regexPattern = generatedRegex
                            }
                        ) {
                            Text("Use Generated")
                        }
                    }
                }

                // Quick patterns
                Text(
                    text = "Quick Patterns:",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start)
                ) {
                    listOf(
                        "Email" to "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}",
                        "IP" to "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$",
                        "URL" to "https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?",
                        "Phone" to "^[\\+]?[1-9][\\d\\-\\(\\)\\s]{7,15}$"
                    ).forEach { (name, pattern) ->
                        Button(
                            onClick = { regexPattern = pattern },
                            //modifier = Modifier.height(32.dp)
                        ) {
                            Text(name, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Analysis results
        if (analysisResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Text Analysis",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    SelectionContainer {
                        Text(
                            text = analysisResults,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Generated regular expression
        if (generatedRegex.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Generated RegEx",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    SelectionContainer {
                        Text(
                            text = generatedRegex,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(4.dp)
                                )
                                .border(
                                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp)
                                .fillMaxWidth(),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Test results
        if (testResults.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Test Results",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )

                    SelectionContainer {
                        Text(
                            text = testResults,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surface,
                                    RoundedCornerShape(4.dp)
                                )
                                .padding(12.dp)
                                .fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private fun analyzeText(text: String): String {
    if (text.isEmpty()) return "Text is empty"

    val lines = text.split('\n')
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

    val analysis = StringBuilder()
    analysis.append("=== GENERAL ANALYSIS ===\n")
    analysis.append("Character count: ${text.length}\n")
    analysis.append("Line count: ${lines.size}\n")
    analysis.append("Word count: ${words.size}\n")
    analysis.append("\n")

    // Pattern analysis
    analysis.append("=== DETECTED PATTERNS ===\n")

    // Email addresses
    val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    val emails = emailPattern.findAll(text).map { it.value }.toSet()
    if (emails.isNotEmpty()) {
        analysis.append("📧 Email addresses (${emails.size}):\n")
        emails.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // URL addresses
    val urlPattern = Regex("https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?")
    val urls = urlPattern.findAll(text).map { it.value }.toSet()
    if (urls.isNotEmpty()) {
        analysis.append("🌐 URL addresses (${urls.size}):\n")
        urls.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // IP addresses
    val ipPattern = Regex("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")
    val ips = ipPattern.findAll(text).map { it.value }.toSet()
    if (ips.isNotEmpty()) {
        analysis.append("🌍 IP addresses (${ips.size}):\n")
        ips.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Phone numbers
    val phonePattern = Regex("[\\+]?[1-9][\\d\\-\\(\\)\\s]{7,15}")
    val phones = phonePattern.findAll(text).map { it.value }.toSet()
    if (phones.isNotEmpty()) {
        analysis.append("📱 Phone numbers (${phones.size}):\n")
        phones.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Dates
    val datePatterns = listOf(
        Regex("\\d{1,2}/\\d{1,2}/\\d{4}"), // MM/dd/yyyy
        Regex("\\d{1,2}-\\d{1,2}-\\d{4}"), // MM-dd-yyyy
        Regex("\\d{4}-\\d{1,2}-\\d{1,2}"), // yyyy-MM-dd
        Regex("\\d{1,2}\\.\\d{1,2}\\.\\d{4}") // dd.MM.yyyy
    )
    val dates = mutableSetOf<String>()
    datePatterns.forEach { pattern ->
        dates.addAll(pattern.findAll(text).map { it.value })
    }
    if (dates.isNotEmpty()) {
        analysis.append("📅 Dates (${dates.size}):\n")
        dates.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Numbers
    val numberPattern = Regex("-?\\d+(?:\\.\\d+)?")
    val numbers = numberPattern.findAll(text).map { it.value }.toSet()
    if (numbers.isNotEmpty() && numbers.size <= 20) {
        analysis.append("🔢 Numbers (${numbers.size}):\n")
        numbers.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Structural analysis
    analysis.append("=== STRUCTURAL ANALYSIS ===\n")
    val hasNumbers = text.any { it.isDigit() }
    val hasLetters = text.any { it.isLetter() }
    val hasSpecialChars = text.any { !it.isLetterOrDigit() && !it.isWhitespace() }

    analysis.append("Contains digits: ${if (hasNumbers) "Yes" else "No"}\n")
    analysis.append("Contains letters: ${if (hasLetters) "Yes" else "No"}\n")
    analysis.append("Contains special characters: ${if (hasSpecialChars) "Yes" else "No"}\n")

    // Character distribution
    val charCounts = text.groupingBy { it }.eachCount()
        .filterKeys { !it.isWhitespace() }
        .toList()
        .sortedByDescending { it.second }
        .take(10)

    if (charCounts.isNotEmpty()) {
        analysis.append("\nMost frequent characters:\n")
        charCounts.forEach { (char, count) ->
            analysis.append("  '$char': $count times\n")
        }
    }

    return analysis.toString()
}

private fun generateRegexFromText(text: String): String {
    if (text.isEmpty()) return ""

    val lines = text.split('\n').filter { it.isNotEmpty() }
    if (lines.isEmpty()) return ""

    // If only one line, analyze it
    if (lines.size == 1) {
        return generateRegexForSingleLine(lines[0])
    }

    // For multiple lines, try to find common pattern
    return generateRegexForMultipleLines(lines)
}

private fun generateRegexForSingleLine(line: String): String {
    // Check for popular patterns
    val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    if (emailPattern.matches(line)) {
        return "[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}"
    }

    val urlPattern = Regex("https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?")
    if (urlPattern.matches(line)) {
        return "https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?"
    }

    val ipPattern = Regex("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")
    if (ipPattern.matches(line)) {
        return "(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)"
    }

    // General approach - create pattern based on structure
    return createPatternFromStructure(line)
}

private fun generateRegexForMultipleLines(lines: List<String>): String {
    // Find common characteristics of all lines
    val minLength = lines.minOf { it.length }
    val maxLength = lines.maxOf { it.length }

    // If all lines have the same length, look for positional patterns
    if (minLength == maxLength) {
        return createFixedLengthPattern(lines)
    }

    // Otherwise create a more general pattern
    val patterns = lines.map { createPatternFromStructure(it) }

    // If all patterns are the same, return one
    if (patterns.toSet().size == 1) {
        return patterns.first()
    }

    // Try to find common elements
    return combinePatterns(patterns)
}

private fun createPatternFromStructure(text: String): String {
    val result = StringBuilder()
    var i = 0

    while (i < text.length) {
        val char = text[i]

        when {
            char.isDigit() -> {
                // Count consecutive digits
                var digitCount = 0
                var j = i
                while (j < text.length && text[j].isDigit()) {
                    digitCount++
                    j++
                }

                when {
                    digitCount == 1 -> result.append("\\d")
                    digitCount <= 3 -> result.append("\\d{$digitCount}")
                    else -> result.append("\\d{$digitCount,}")
                }
                i = j
            }

            char.isLetter() -> {
                // Count consecutive letters
                var letterCount = 0
                var j = i
                val isUpper = char.isUpperCase()

                while (j < text.length && text[j].isLetter() && text[j].isUpperCase() == isUpper) {
                    letterCount++
                    j++
                }

                val pattern = if (isUpper) "[A-Z]" else "[a-z]"
                when {
                    letterCount == 1 -> result.append(pattern)
                    letterCount <= 3 -> result.append("$pattern{$letterCount}")
                    else -> result.append("$pattern{$letterCount,}")
                }
                i = j
            }

            char.isWhitespace() -> {
                result.append("\\s+")
                // Skip all consecutive spaces
                while (i < text.length && text[i].isWhitespace()) {
                    i++
                }
            }

            else -> {
                // Escape special characters
                val escaped = when (char) {
                    '.', '*', '+', '?', '^', '$', '(', ')', '[', ']', '{', '}', '|', '\\' -> "\\$char"
                    else -> char.toString()
                }
                result.append(escaped)
                i++
            }
        }
    }

    return result.toString()
}

private fun createFixedLengthPattern(lines: List<String>): String {
    val length = lines.first().length
    val result = StringBuilder()

    for (pos in 0 until length) {
        val charsAtPos = lines.map { it[pos] }.toSet()

        when {
            charsAtPos.size == 1 -> {
                // All characters at this position are the same
                val char = charsAtPos.first()
                val escaped = when (char) {
                    '.', '*', '+', '?', '^', '$', '(', ')', '[', ']', '{', '}', '|', '\\' -> "\\$char"
                    else -> char.toString()
                }
                result.append(escaped)
            }

            charsAtPos.all { it.isDigit() } -> {
                result.append("\\d")
            }

            charsAtPos.all { it.isLetter() } -> {
                if (charsAtPos.all { it.isUpperCase() }) {
                    result.append("[A-Z]")
                } else if (charsAtPos.all { it.isLowerCase() }) {
                    result.append("[a-z]")
                } else {
                    result.append("[a-zA-Z]")
                }
            }

            charsAtPos.all { it.isLetterOrDigit() } -> {
                result.append("[a-zA-Z0-9]")
            }

            else -> {
                // Create character class
                val sortedChars = charsAtPos.sorted()
                result.append("[${sortedChars.joinToString("") {
                    when (it) {
                        ']', '\\', '^', '-' -> "\\$it"
                        else -> it.toString()
                    }
                }}]")
            }
        }
    }

    return result.toString()
}

private fun combinePatterns(patterns: List<String>): String {
    // Simplified pattern combination logic
    // Return first pattern with note that this is one of possible variants
    return if (patterns.isNotEmpty()) {
        "(?:" + patterns.joinToString("|") + ")"
    } else {
        ".*"
    }
}

private fun testRegex(pattern: String, text: String): String {
    if (pattern.isEmpty() || text.isEmpty()) {
        return "Pattern or text for testing not specified"
    }

    val result = StringBuilder()

    try {
        val regex = Regex(pattern)
        val matches = regex.findAll(text).toList()

        result.append("=== TEST RESULTS ===\n")
        result.append("Pattern: $pattern\n")
        result.append("Matches found: ${matches.size}\n\n")

        if (matches.isNotEmpty()) {
            result.append("=== FOUND MATCHES ===\n")
            matches.forEachIndexed { index, match ->
                result.append("${index + 1}. \"${match.value}\" (position ${match.range.first}-${match.range.last})\n")

                if (match.groups.size > 1) {
                    result.append("   Groups:\n")
                    match.groups.drop(1).forEachIndexed { groupIndex, group ->
                        if (group != null) {
                            result.append("   - Group ${groupIndex + 1}: \"${group.value}\"\n")
                        }
                    }
                }
            }
            result.append("\n")

            // Statistics
            result.append("=== STATISTICS ===\n")
            val uniqueMatches = matches.map { it.value }.toSet()
            result.append("Unique matches: ${uniqueMatches.size}\n")

            if (uniqueMatches.size != matches.size) {
                result.append("Repeated matches:\n")
                val duplicates = matches.map { it.value }
                    .groupingBy { it }
                    .eachCount()
                    .filter { it.value > 1 }

                duplicates.forEach { (value, count) ->
                    result.append("  \"$value\": $count times\n")
                }
            }
        } else {
            result.append("❌ No matches found\n")
            result.append("\nTips for improving the pattern:\n")
            result.append("- Check the regular expression syntax\n")
            result.append("- Simplify the pattern for initial testing\n")
            result.append("- Use text analysis to find patterns\n")
        }

    } catch (e: Exception) {
        result.append("❌ ERROR DURING TESTING\n")
        result.append("${e.message}\n")
        result.append("\nCheck the regular expression syntax.")
    }

    return result.toString()
}