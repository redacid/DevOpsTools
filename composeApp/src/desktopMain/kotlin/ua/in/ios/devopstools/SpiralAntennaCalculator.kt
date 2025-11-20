package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.*

data class AntennaInfo(
    val frequency: Double,
    val wavelength: Double,
    val totalLength: Double,
    val turns: Int,
    val coilDiameter: Double,
    val wireDiameter: Double,
    val pitch: Double,
    val inductance: Double,
    val gain: Double,
    val power: Double,
    val coilHeight: Double
)

enum class CalculationMode {
    AUTO,           // Автоматичний розрахунок
    FIXED_DIAMETER, // Фіксований діаметр
    FIXED_TURNS     // Фіксована кількість витків
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiralAntennaCalculator(modifier: Modifier = Modifier) {
    var frequency by remember { mutableStateOf("433.0") }
    var wireDiameter by remember { mutableStateOf("1.0") }
    var power by remember { mutableStateOf("10.0") }
    var calculationMode by remember { mutableStateOf(CalculationMode.AUTO) }
    var fixedCoilDiameter by remember { mutableStateOf("5.0") }
    var fixedTurns by remember { mutableStateOf("5") }
    var calculationResult by remember { mutableStateOf<AntennaInfo?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Автоматичний пересчет при зміні параметрів
    LaunchedEffect(frequency, wireDiameter, power, calculationMode, fixedCoilDiameter, fixedTurns) {
        if (frequency.isNotBlank() && wireDiameter.isNotBlank() && power.isNotBlank()) {
            try {
                val result = calculateSpiralAntenna(
                    frequency.toDouble(),
                    wireDiameter.toDouble(),
                    power.toDouble(),
                    calculationMode,
                    if (calculationMode == CalculationMode.FIXED_DIAMETER) fixedCoilDiameter.toDoubleOrNull() else null,
                    if (calculationMode == CalculationMode.FIXED_TURNS) fixedTurns.toIntOrNull() else null
                )
                calculationResult = result
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Помилка: ${e.message}"
                calculationResult = null
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Калькулятор спіральної антени",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Розрахунок параметрів спіральної антени для приймачів та передавачів (наприклад, гаражні ворота)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Основні параметри
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Основні параметри",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Поле для введення частоти
                OutlinedTextField(
                    value = frequency,
                    onValueChange = {
                        frequency = it
                        errorMessage = ""
                    },
                    label = { Text("Частота (МГц)") },
                    placeholder = { Text("433.0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Стандартні частоти: 433 МГц, 315 МГц, 868 МГц") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Поле для введення діаметру дроту
                OutlinedTextField(
                    value = wireDiameter,
                    onValueChange = {
                        wireDiameter = it
                        errorMessage = ""
                    },
                    label = { Text("Діаметр дроту (мм)") },
                    placeholder = { Text("1.0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Типові значення: 0.5-2.0 мм") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Поле для введення потужності
                OutlinedTextField(
                    value = power,
                    onValueChange = {
                        power = it
                        errorMessage = ""
                    },
                    label = { Text("Потужність передавача (Вт)") },
                    placeholder = { Text("10.0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Для гаражних воріт зазвичай 1-50 Вт") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Режим розрахунку
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Режим розрахунку",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Радіокнопки для вибору режиму
                Column {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = calculationMode == CalculationMode.AUTO,
                            onClick = { calculationMode = CalculationMode.AUTO }
                        )
                        Text(
                            text = "Автоматичний розрахунок (оптимальні значення)",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = calculationMode == CalculationMode.FIXED_DIAMETER,
                            onClick = { calculationMode = CalculationMode.FIXED_DIAMETER }
                        )
                        Text(
                            text = "Задати діаметр котушки",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = calculationMode == CalculationMode.FIXED_TURNS,
                            onClick = { calculationMode = CalculationMode.FIXED_TURNS }
                        )
                        Text(
                            text = "Задати кількість витків",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Додаткові поля в залежності від режиму
                when (calculationMode) {
                    CalculationMode.FIXED_DIAMETER -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fixedCoilDiameter,
                            onValueChange = { fixedCoilDiameter = it },
                            label = { Text("Внутрішній діаметр котушки (см)") },
                            placeholder = { Text("5.0") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = { Text("Діаметр основи для намотування") }
                        )
                    }
                    CalculationMode.FIXED_TURNS -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fixedTurns,
                            onValueChange = { fixedTurns = it },
                            label = { Text("Кількість витків") },
                            placeholder = { Text("5") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Рекомендовано: 3-10 витків") }
                        )
                    }
                    CalculationMode.AUTO -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Параметри котушки будуть розраховані автоматично для оптимальної роботи",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Відображення помилок
        if (errorMessage.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.padding(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Відображення результатів
        calculationResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Результати розрахунку",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SelectionContainer {
                        Column {
                            AntennaInfoRow("Частота:", "${result.frequency} МГц")
                            AntennaInfoRow("Довжина хвилі:", "${String.format("%.3f", result.wavelength)} м")
                            AntennaInfoRow("Загальна довжина дроту:", "${String.format("%.3f", result.totalLength)} м")
                            AntennaInfoRow("Кількість витків:", "${result.turns}")
                            AntennaInfoRow("Внутрішній діаметр котушки:", "${String.format("%.2f", result.coilDiameter)} см")
                            AntennaInfoRow("Висота котушки:", "${String.format("%.2f", result.coilHeight)} см")
                            AntennaInfoRow("Діаметр дроту:", "${result.wireDiameter} мм")
                            AntennaInfoRow("Крок спіралі:", "${String.format("%.2f", result.pitch)} мм")
                            AntennaInfoRow("Індуктивність:", "${String.format("%.2f", result.inductance)} мкГн")
                            AntennaInfoRow("Очікуваний коефіцієнт підсилення:", "${String.format("%.1f", result.gain)} дБi")
                            AntennaInfoRow("Потужність:", "${result.power} Вт")
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Додаткова інформація
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Рекомендації для виготовлення",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = buildRecommendations(result),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun AntennaInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun buildRecommendations(result: AntennaInfo): String {
    return buildString {
        append("• Використовуйте мідний дріт для кращої провідності\n")
        append("• Основа для намотування: труба діаметром ${String.format("%.1f", result.coilDiameter)} см\n")
        append("• Висота котушки: ${String.format("%.1f", result.coilHeight)} см\n")
        append("• Рівномірно розподіліть ${result.turns} витків по висоті котушки\n")
        append("• Крок між витками: ${String.format("%.1f", result.pitch)} мм\n")
        append("• Підключіть центральний провідник до одного кінця, екран - до іншого\n")
        append("• Для ${result.frequency} МГц оптимальна поляризація - вертикальна\n")
        append("• Розташовуйте антену якомога вище над землею\n")

        if (result.frequency == 433.0) {
            append("• Для 433 МГц: оптимальна дальність до 500м на відкритій місцевості")
        }
    }
}

private fun calculateSpiralAntenna(
    frequencyMHz: Double,
    wireDiameterMm: Double,
    powerW: Double,
    mode: CalculationMode,
    fixedDiameterCm: Double? = null,
    fixedTurns: Int? = null
): AntennaInfo {
    // Валідація вхідних даних
    if (frequencyMHz <= 0) throw IllegalArgumentException("Частота повинна бути більше 0")
    if (wireDiameterMm <= 0) throw IllegalArgumentException("Діаметр дроту повинен бути більше 0")
    if (powerW <= 0) throw IllegalArgumentException("Потужність повинна бути більше 0")

    // Швидкість світла (м/с)
    val lightSpeed = 299_792_458.0

    // Розрахунок довжини хвилі
    val wavelength = lightSpeed / (frequencyMHz * 1_000_000)

    // Оптимальна загальна довжина дроту для спіральної антени (1/4 довжини хвилі)
    val totalLength = wavelength / 4

    // Розрахунок параметрів в залежності від режиму
    val (coilDiameter, turns) = when (mode) {
        CalculationMode.FIXED_DIAMETER -> {
            if (fixedDiameterCm == null || fixedDiameterCm <= 0) {
                throw IllegalArgumentException("Діаметр котушки повинен бути більше 0")
            }
            val circumference = PI * (fixedDiameterCm / 100) // окружність в метрах
            val calculatedTurns = (totalLength / circumference).toInt().coerceAtLeast(2)
            Pair(fixedDiameterCm, calculatedTurns)
        }
        CalculationMode.FIXED_TURNS -> {
            if (fixedTurns == null || fixedTurns <= 0) {
                throw IllegalArgumentException("Кількість витків повинна бути більше 0")
            }
            val circumference = totalLength / fixedTurns
            val calculatedDiameter = (circumference / PI) * 100 // в сантиметрах
            Pair(calculatedDiameter, fixedTurns)
        }
        CalculationMode.AUTO -> {
            // Автоматичний розрахунок оптимального діаметру (приблизно 1/10 довжини хвилі)
            val optimalDiameter = (wavelength / 10) * 100 // в сантиметрах
            val circumference = PI * (optimalDiameter / 100)
            val estimatedTurns = (totalLength / circumference).toInt().coerceAtLeast(3)

            // Уточнення кількості витків для оптимізації
            val optimizedTurns = when {
                frequencyMHz < 100 -> estimatedTurns.coerceAtLeast(5)
                frequencyMHz < 500 -> estimatedTurns.coerceAtLeast(3)
                else -> estimatedTurns.coerceAtLeast(2)
            }
            Pair(optimalDiameter, optimizedTurns)
        }
    }

    // Розрахунок висоти котушки та кроку спіралі
    val actualCircumference = PI * (coilDiameter / 100)
    val actualWireLength = actualCircumference * turns
    val coilHeight = sqrt(totalLength * totalLength - actualWireLength * actualWireLength).coerceAtLeast(actualWireLength * 0.1)
    val pitch = if (turns > 1) (coilHeight * 1000) / (turns - 1) else coilHeight * 1000

    // Розрахунок індуктивності (формула для багатовиткової котушки)
    val radiusCm = coilDiameter / 2
    val lengthCm = coilHeight * 100
    val inductance = (radiusCm * radiusCm * turns * turns) / (22.86 * radiusCm + 25.4 * lengthCm)

    // Розрахунок коефіцієнта підсилення (приблизний для спіральної антени)
    val gain = 10 * log10(4 * PI * turns * (coilDiameter / 100) / wavelength)

    return AntennaInfo(
        frequency = frequencyMHz,
        wavelength = wavelength,
        totalLength = totalLength,
        turns = turns,
        coilDiameter = coilDiameter,
        wireDiameter = wireDiameterMm,
        pitch = pitch,
        inductance = inductance,
        gain = gain.coerceAtLeast(0.0),
        power = powerW,
        coilHeight = coilHeight
    )
}