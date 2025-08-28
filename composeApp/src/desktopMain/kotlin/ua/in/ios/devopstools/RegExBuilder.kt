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
import androidx.compose.ui.graphics.Color
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
            text = "Генератор/Тестер Регулярних Виразів",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        // Секція вводу тексту
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Текст для аналізу",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    placeholder = { Text("Вставте ваш текст тут для аналізу та генерації регулярних виразів...") },
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
                        Text("Аналізувати")
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
                        Text("Генерувати RegEx")
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
                        Text("Очистити")
                    }
                }
            }
        }

        // Секція регулярного виразу
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Регулярний вираз",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )

                OutlinedTextField(
                    value = regexPattern,
                    onValueChange = { newValue ->
                        regexPattern = newValue
                        // Валідація regex в реальному часі
                        try {
                            Pattern.compile(newValue)
                            isValidRegex = true
                            regexError = ""
                        } catch (e: PatternSyntaxException) {
                            isValidRegex = false
                            regexError = e.description ?: "Невірний синтаксис"
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Введіть регулярний вираз або використайте згенерований...") },
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
                        Text("Тестувати")
                    }

                    if (generatedRegex.isNotEmpty()) {
                        Button(
                            onClick = {
                                regexPattern = generatedRegex
                            }
                        ) {
                            Text("Використати згенерований")
                        }
                    }
                }

                // Швидкі патерни
                Text(
                    text = "Швидкі патерни:",
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
                        "Телефон" to "^[\\+]?[1-9][\\d\\-\\(\\)\\s]{7,15}$"
                    ).forEach { (name, pattern) ->
                        Button(
                            onClick = { regexPattern = pattern },
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(name, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Результати аналізу
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
                        text = "Аналіз тексту",
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

        // Згенерований регулярний вираз
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
                        text = "Згенерований RegEx",
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

        // Результати тестування
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
                        text = "Результати тестування",
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
    if (text.isEmpty()) return "Текст порожній"

    val lines = text.split('\n')
    val words = text.split(Regex("\\s+")).filter { it.isNotEmpty() }

    val analysis = StringBuilder()
    analysis.append("=== ЗАГАЛЬНИЙ АНАЛІЗ ===\n")
    analysis.append("Кількість символів: ${text.length}\n")
    analysis.append("Кількість рядків: ${lines.size}\n")
    analysis.append("Кількість слів: ${words.size}\n")
    analysis.append("\n")

    // Аналіз патернів
    analysis.append("=== ВИЯВЛЕНІ ПАТЕРНИ ===\n")

    // Email адреси
    val emailPattern = Regex("[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}")
    val emails = emailPattern.findAll(text).map { it.value }.toSet()
    if (emails.isNotEmpty()) {
        analysis.append("📧 Email адреси (${emails.size}):\n")
        emails.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // URL адреси
    val urlPattern = Regex("https?://[\\w\\-]+(\\.[\\w\\-]+)+([\\w\\-\\.,@?^=%&:/~\\+#]*[\\w\\-\\@?^=%&/~\\+#])?")
    val urls = urlPattern.findAll(text).map { it.value }.toSet()
    if (urls.isNotEmpty()) {
        analysis.append("🌐 URL адреси (${urls.size}):\n")
        urls.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // IP адреси
    val ipPattern = Regex("(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)")
    val ips = ipPattern.findAll(text).map { it.value }.toSet()
    if (ips.isNotEmpty()) {
        analysis.append("🌍 IP адреси (${ips.size}):\n")
        ips.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Номери телефонів
    val phonePattern = Regex("[\\+]?[1-9][\\d\\-\\(\\)\\s]{7,15}")
    val phones = phonePattern.findAll(text).map { it.value }.toSet()
    if (phones.isNotEmpty()) {
        analysis.append("📱 Номери телефонів (${phones.size}):\n")
        phones.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Дати
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
        analysis.append("📅 Дати (${dates.size}):\n")
        dates.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Числа
    val numberPattern = Regex("-?\\d+(?:\\.\\d+)?")
    val numbers = numberPattern.findAll(text).map { it.value }.toSet()
    if (numbers.isNotEmpty() && numbers.size <= 20) {
        analysis.append("🔢 Числа (${numbers.size}):\n")
        numbers.forEach { analysis.append("  - $it\n") }
        analysis.append("\n")
    }

    // Структурний аналіз
    analysis.append("=== СТРУКТУРНИЙ АНАЛІЗ ===\n")
    val hasNumbers = text.any { it.isDigit() }
    val hasLetters = text.any { it.isLetter() }
    val hasSpecialChars = text.any { !it.isLetterOrDigit() && !it.isWhitespace() }

    analysis.append("Містить цифри: ${if (hasNumbers) "Так" else "Ні"}\n")
    analysis.append("Містить літери: ${if (hasLetters) "Так" else "Ні"}\n")
    analysis.append("Містить спеціальні символи: ${if (hasSpecialChars) "Так" else "Ні"}\n")

    // Розподіл символів
    val charCounts = text.groupingBy { it }.eachCount()
        .filterKeys { !it.isWhitespace() }
        .toList()
        .sortedByDescending { it.second }
        .take(10)

    if (charCounts.isNotEmpty()) {
        analysis.append("\nНайчастіші символи:\n")
        charCounts.forEach { (char, count) ->
            analysis.append("  '$char': $count разів\n")
        }
    }

    return analysis.toString()
}

private fun generateRegexFromText(text: String): String {
    if (text.isEmpty()) return ""

    val lines = text.split('\n').filter { it.isNotEmpty() }
    if (lines.isEmpty()) return ""

    // Якщо всього один рядок, аналізуємо його
    if (lines.size == 1) {
        return generateRegexForSingleLine(lines[0])
    }

    // Для множинних рядків намагаємось знайти спільний патерн
    return generateRegexForMultipleLines(lines)
}

private fun generateRegexForSingleLine(line: String): String {
    // Перевіряємо на популярні патерни
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

    // Загальний підхід - створюємо патерн на основі структури
    return createPatternFromStructure(line)
}

private fun generateRegexForMultipleLines(lines: List<String>): String {
    // Знаходимо спільні характеристики всіх рядків
    val minLength = lines.minOf { it.length }
    val maxLength = lines.maxOf { it.length }

    // Якщо всі рядки однакової довжини, шукаємо позиційні патерни
    if (minLength == maxLength) {
        return createFixedLengthPattern(lines)
    }

    // Інакше створюємо більш загальний патерн
    val patterns = lines.map { createPatternFromStructure(it) }

    // Якщо всі патерни однакові, повертаємо один
    if (patterns.toSet().size == 1) {
        return patterns.first()
    }

    // Намагаємось знайти спільні елементи
    return combinePatterns(patterns)
}

private fun createPatternFromStructure(text: String): String {
    val result = StringBuilder()
    var i = 0

    while (i < text.length) {
        val char = text[i]

        when {
            char.isDigit() -> {
                // Рахуємо послідовні цифри
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
                // Рахуємо послідовні літери
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
                // Пропускаємо всі послідовні пробіли
                while (i < text.length && text[i].isWhitespace()) {
                    i++
                }
            }

            else -> {
                // Спеціальні символи екрануємо
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
                // Всі символи на цій позиції однакові
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
                // Створюємо клас символів
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
    // Спрощена логіка комбінування патернів
    // Повертаємо перший патерн з припискою що це один з можливих варіантів
    return if (patterns.isNotEmpty()) {
        "(?:" + patterns.joinToString("|") + ")"
    } else {
        ".*"
    }
}

private fun testRegex(pattern: String, text: String): String {
    if (pattern.isEmpty() || text.isEmpty()) {
        return "Не вказано патерн або текст для тестування"
    }

    val result = StringBuilder()

    try {
        val regex = Regex(pattern)
        val matches = regex.findAll(text).toList()

        result.append("=== РЕЗУЛЬТАТИ ТЕСТУВАННЯ ===\n")
        result.append("Патерн: $pattern\n")
        result.append("Знайдено збігів: ${matches.size}\n\n")

        if (matches.isNotEmpty()) {
            result.append("=== ЗНАЙДЕНІ ЗБІГИ ===\n")
            matches.forEachIndexed { index, match ->
                result.append("${index + 1}. \"${match.value}\" (позиція ${match.range.first}-${match.range.last})\n")

                if (match.groups.size > 1) {
                    result.append("   Групи:\n")
                    match.groups.drop(1).forEachIndexed { groupIndex, group ->
                        if (group != null) {
                            result.append("   - Група ${groupIndex + 1}: \"${group.value}\"\n")
                        }
                    }
                }
            }
            result.append("\n")

            // Статистика
            result.append("=== СТАТИСТИКА ===\n")
            val uniqueMatches = matches.map { it.value }.toSet()
            result.append("Унікальних збігів: ${uniqueMatches.size}\n")

            if (uniqueMatches.size != matches.size) {
                result.append("Повторювані збіги:\n")
                val duplicates = matches.map { it.value }
                    .groupingBy { it }
                    .eachCount()
                    .filter { it.value > 1 }

                duplicates.forEach { (value, count) ->
                    result.append("  \"$value\": $count разів\n")
                }
            }
        } else {
            result.append("❌ Збігів не знайдено\n")
            result.append("\nПоради для покращення патерну:\n")
            result.append("- Перевірте синтаксис регулярного виразу\n")
            result.append("- Спростіть патерн для початкового тестування\n")
            result.append("- Використайте аналіз тексту для знаходження патернів\n")
        }

    } catch (e: Exception) {
        result.append("❌ ПОМИЛКА ПРИ ТЕСТУВАННІ\n")
        result.append("${e.message}\n")
        result.append("\nПеревірте синтаксис регулярного виразу.")
    }

    return result.toString()
}