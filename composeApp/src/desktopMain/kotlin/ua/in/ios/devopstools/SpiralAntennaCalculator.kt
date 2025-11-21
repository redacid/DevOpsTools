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
    val coilDiameterMm: Double,     // в мм
    val wireDiameterMm: Double,     // в мм
    val pitchMm: Double,            // в мм
    val inductance: Double,
    val gain: Double,
    val power: Double,
    val coilHeightMm: Double,       // в мм
    val eirp: Double,
    val range: Double,              // в метрах
    val impedance: Double,
    val bandwidth: Double,
    val efficiency: Double
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
    var wireDiameter by remember { mutableStateOf("0.5") }
    var power by remember { mutableStateOf("10.0") }
    var calculationMode by remember { mutableStateOf(CalculationMode.AUTO) }
    var fixedCoilDiameter by remember { mutableStateOf("50.0") }
    var fixedTurns by remember { mutableStateOf("5") }
    var pitchBetweenCenters by remember { mutableStateOf("1.0") } // новий параметр
    var calculationResult by remember { mutableStateOf<AntennaInfo?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Автоматичний пересчет при зміні параметрів
    LaunchedEffect(frequency, wireDiameter, power, calculationMode, fixedCoilDiameter, fixedTurns, pitchBetweenCenters) {
        if (frequency.isNotBlank() && wireDiameter.isNotBlank() && power.isNotBlank() && pitchBetweenCenters.isNotBlank()) {
            try {
                val result = calculateSpiralAntenna(
                    frequency.toDouble(),
                    wireDiameter.toDouble(),
                    power.toDouble(),
                    calculationMode,
                    if (calculationMode == CalculationMode.FIXED_DIAMETER) fixedCoilDiameter.toDoubleOrNull() else null,
                    if (calculationMode == CalculationMode.FIXED_TURNS) fixedTurns.toIntOrNull() else null,
                    pitchBetweenCenters.toDouble()
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
                    placeholder = { Text("0.5") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Типові значення: 0.3-2.0 мм") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Поле для введення кроку між центрами витків
                OutlinedTextField(
                    value = pitchBetweenCenters,
                    onValueChange = {
                        pitchBetweenCenters = it
                        errorMessage = ""
                    },
                    label = { Text("Крок між центрами витків (мм)") },
                    placeholder = { Text("1.0") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Відстань між центрами сусідніх витків. Мінімум = діаметр дроту") }
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
                            label = { Text("Внутрішній діаметр котушки (мм)") },
                            placeholder = { Text("50.0") },
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
                            supportingText = { Text("Рекомендовано: 3-15 витків") }
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
                            AntennaInfoRow("Довжина хвилі:", "${String.format("%.0f", result.wavelength * 1000)} мм")
                            AntennaInfoRow("Загальна довжина дроту:", "${String.format("%.0f", result.totalLength * 1000)} мм")
                            AntennaInfoRow("Кількість витків:", "${result.turns}")
                            AntennaInfoRow("Внутрішній діаметр котушки:", "${String.format("%.1f", result.coilDiameterMm)} мм")
                            AntennaInfoRow("Висота котушки:", "${String.format("%.1f", result.coilHeightMm)} мм")
                            AntennaInfoRow("Діаметр дроту:", "${String.format("%.1f", result.wireDiameterMm)} мм")
                            AntennaInfoRow("Крок спіралі:", "${String.format("%.1f", result.pitchMm)} мм")

                            // Розділювач для електричних характеристик
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            AntennaInfoRow("Потужність передавача:", "${result.power} Вт")
                            AntennaInfoRow("EIRP (ефективна потужність):", "${String.format("%.3f", result.eirp)} Вт")
                            AntennaInfoRow("Коефіцієнт підсилення:", "${String.format("%.1f", result.gain)} дБi")
                            AntennaInfoRow("КПД антени:", "${String.format("%.1f", result.efficiency * 100)}%")
                            AntennaInfoRow("Вхідний опір:", "${String.format("%.0f", result.impedance)} Ом")
                            AntennaInfoRow("Смуга пропускання:", "${String.format("%.1f", result.bandwidth)} МГц")
                            AntennaInfoRow("Приблизна дальність дії:", "${String.format("%.0f", result.range)} м")

                            // Індуктивність винесемо окремо
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            AntennaInfoRow("Індуктивність:", "${String.format("%.2f", result.inductance)} мкГн")
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



private fun calculateSpiralAntenna(
    frequencyMHz: Double,
    wireDiameterMm: Double,
    powerW: Double,
    mode: CalculationMode,
    fixedCoilDiameterMm: Double? = null,
    fixedTurns: Int? = null,
    pitchBetweenCentersMm: Double = 1.0
): AntennaInfo {
    // Валідація вхідних даних
    if (frequencyMHz <= 0) throw IllegalArgumentException("Частота повинна бути більше 0")
    if (wireDiameterMm <= 0) throw IllegalArgumentException("Діаметр дроту повинен бути більше 0")
    if (powerW <= 0) throw IllegalArgumentException("Потужність повинна бути більше 0")
    if (pitchBetweenCentersMm < wireDiameterMm) throw IllegalArgumentException("Крок між витками не може бути менше діаметра дроту")

    // Швидкість світла (м/с)
    val lightSpeed = 299_792_458.0

    // Розрахунок довжини хвилі
    val wavelength = lightSpeed / (frequencyMHz * 1_000_000)

    // Для спіральної антени оптимальна довжина - близько 1/4 довжини хвилі
    val totalLength = wavelength / 4

    // Розрахунок параметрів в залежності від режиму
    val (coilDiameterMm, turns) = when (mode) {
        CalculationMode.FIXED_DIAMETER -> {
            if (fixedCoilDiameterMm == null || fixedCoilDiameterMm <= 0) {
                throw IllegalArgumentException("Діаметр котушки повинен бути більше 0")
            }
            // Розрахунок кількості витків для заданого діаметру
            val circumferenceMm = PI * fixedCoilDiameterMm
            val calculatedTurns = (totalLength * 1000 / circumferenceMm).toInt().coerceIn(2, 30)
            Pair(fixedCoilDiameterMm, calculatedTurns)
        }
        CalculationMode.FIXED_TURNS -> {
            if (fixedTurns == null || fixedTurns <= 0) {
                throw IllegalArgumentException("Кількість витків повинна бути більше 0")
            }
            // Розрахунок діаметру для заданої кількості витків
            val circumferenceMm = totalLength * 1000 / fixedTurns
            val calculatedDiameterMm = circumferenceMm / PI
            Pair(calculatedDiameterMm, fixedTurns)
        }
        CalculationMode.AUTO -> {
            // Для автоматичного режиму використовуємо практичні правила
            val optimalDiameterMm = when {
                frequencyMHz < 200 -> wavelength * 150  // 15% від довжини хвилі в мм
                frequencyMHz < 500 -> wavelength * 120  // 12% від довжини хвилі в мм
                else -> wavelength * 100                // 10% від довжини хвилі в мм
            }.coerceIn(20.0, 150.0) // обмежуємо 2-15 см

            val circumferenceMm = PI * optimalDiameterMm
            val estimatedTurns = (totalLength * 1000 / circumferenceMm).toInt().coerceIn(3, 12)

            Pair(optimalDiameterMm, estimatedTurns)
        }
    }

    // Розрахунок висоти котушки
    val coilHeightMm = (turns - 1) * pitchBetweenCentersMm // висота = (витки - 1) × крок

    // Розрахунок реальної довжини дроту
    val circumferenceMm = PI * coilDiameterMm
    val wireLengthPerTurn = sqrt(circumferenceMm.pow(2) + pitchBetweenCentersMm.pow(2))
    val actualTotalLength = wireLengthPerTurn * turns / 1000 // в метрах

    // Розрахунок індуктивності
    val radiusMm = coilDiameterMm / 2
    val aspectRatio = coilHeightMm / coilDiameterMm

    // Коефіцієнт корекції для спіральної форми
    val correctionFactor = when {
        aspectRatio < 0.1 -> 0.4  // дуже плоска котушка
        aspectRatio < 0.5 -> 0.5 + aspectRatio * 0.6
        aspectRatio < 2.0 -> 0.8 + aspectRatio * 0.1
        aspectRatio < 5.0 -> 1.0
        else -> 0.9 // дуже витягнута котушка
    }

    // Формула для індуктивності котушки (результат в мкГн)
    val inductance = correctionFactor * (radiusMm * radiusMm * turns * turns) /
            (22.86 * radiusMm + 25.4 * coilHeightMm)

    // Розрахунок коефіцієнта підсилення
    val circumferenceInWavelengths = (circumferenceMm / 1000) / wavelength
    val heightInWavelengths = (coilHeightMm / 1000) / wavelength

    val gain = when {
        turns <= 2 -> 0.0 + 2.0 * circumferenceInWavelengths
        turns <= 8 -> 1.0 + 5.0 * log10((turns * circumferenceInWavelengths).coerceAtLeast(0.1))
        else -> 2.5 + 7.0 * log10((turns * circumferenceInWavelengths).coerceAtLeast(0.1))
    }.coerceIn(-2.0, 10.0)

    val gainLinear = 10.0.pow(gain / 10.0)

    // Розрахунок КПД
    val skinDepth = sqrt(2 / (2 * PI * frequencyMHz * 1e6 * 4e-7 * PI * 5.8e7))
    val effectiveResistance = if (wireDiameterMm > 2 * skinDepth * 1000) {
        actualTotalLength / (5.8e7 * 2 * PI * (wireDiameterMm / 2000) * skinDepth)
    } else {
        actualTotalLength * 0.0172 / (PI * (wireDiameterMm / 2000).pow(2))
    }

    val proximityLossFactor = 1.0 + (turns - 1) * 0.03 // 3% втрат на кожен додатковий виток
    val totalResistance = effectiveResistance * proximityLossFactor

    val radiationResistance = when {
        actualTotalLength / wavelength < 0.1 -> 10.0 * (actualTotalLength / wavelength).pow(2)
        actualTotalLength / wavelength < 0.5 -> 36.6 * (actualTotalLength / wavelength).pow(2)
        else -> 73.1 * (actualTotalLength / wavelength - 0.25).pow(2) + 36.6 * 0.25.pow(2)
    }

    val efficiency = radiationResistance / (radiationResistance + totalResistance)

    // EIRP
    val eirp = powerW * gainLinear * efficiency

    // Вхідний опір
    val baseImpedance = when {
        turns <= 3 -> 30.0 + 12.0 * turns
        turns <= 8 -> 66.0 + 8.0 * (turns - 3)
        else -> 106.0 + 6.0 * (turns - 8)
    }

    val impedance = baseImpedance.coerceIn(25.0, 200.0)

    // Смуга пропускання
    val q = when {
        efficiency > 0.7 -> 18.0 + turns * 2.5
        efficiency > 0.5 -> 12.0 + turns * 2.0
        efficiency > 0.3 -> 8.0 + turns * 1.5
        else -> 6.0 + turns * 1.0
    }

    val bandwidth = (frequencyMHz / q).coerceAtLeast(0.1)

    // Дальність дії
    val rxSensitivity = when {
        frequencyMHz < 200 -> -110.0
        frequencyMHz < 500 -> -105.0
        frequencyMHz < 1000 -> -100.0
        else -> -95.0
    }

    val txPowerDbm = 10 * log10(powerW * 1000)
    val eirpDbm = txPowerDbm + gain + 10 * log10(efficiency)
    val linkMargin = 20.0
    val availableBudget = eirpDbm - rxSensitivity - linkMargin

    val range = if (availableBudget > 0) {
        lightSpeed / (4 * PI * frequencyMHz * 1e6) * 10.0.pow(availableBudget / 20.0)
    } else {
        10.0
    }.coerceIn(1.0, 1000.0)

    return AntennaInfo(
        frequency = frequencyMHz,
        wavelength = wavelength,
        totalLength = totalLength,
        turns = turns,
        coilDiameterMm = coilDiameterMm,
        wireDiameterMm = wireDiameterMm,
        pitchMm = pitchBetweenCentersMm,
        inductance = inductance,
        gain = gain,
        power = powerW,
        coilHeightMm = coilHeightMm,
        eirp = eirp,
        range = range,
        impedance = impedance,
        bandwidth = bandwidth,
        efficiency = efficiency.coerceIn(0.2, 0.9)
    )
}

private fun buildRecommendations(result: AntennaInfo): String {
    return buildString {
        append("• Використовуйте мідний дріт діаметром ${result.wireDiameterMm} мм\n")
        append("• Основа для намотування: труба діаметром ${String.format("%.1f", result.coilDiameterMm)} мм\n")
        append("• Висота котушки: ${String.format("%.1f", result.coilHeightMm)} мм\n")
        append("• Намотайте ${result.turns} витків з кроком ${String.format("%.1f", result.pitchMm)} мм\n")
        append("• Підключіть центральний провідник до одного кінця, екран - до іншого\n")
        append("• Для ${result.frequency} МГц оптимальна поляризація - вертикальна\n")
        append("• Розташовуйте антену якомога вище над землею\n")

        // Рекомендації залежно від потужності
        when {
            result.power <= 1 -> append("• Низька потужність: підходить для коротких дистанцій\n")
            result.power <= 10 -> append("• Середня потужність: оптимально для гаражних воріт та пультів\n")
            result.power > 10 -> append("• Висока потужність: забезпечте якісне узгодження (КСХ < 1.5)\n")
        }

        if (result.frequency == 433.0) {
            append("• Для 433 МГц ISM діапазону: дотримуйтесь регуляторних вимог")
        }
    }
}