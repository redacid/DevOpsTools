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
    val efficiency: Double,
    // Нові параметри для аналізу товщини дроту
    val skinDepthMm: Double,        // глибина скін-ефекту в мм
    val wireResistanceOhm: Double,  // опір дроту в Ом
    val radiationResistanceOhm: Double, // опір випромінювання в Ом
    val qualityFactor: Double,      // добротність Q
    val skinEffectFactor: Double,   // коефіцієнт скін-ефекту
    val wireQuality: String         // оцінка якості товщини дроту
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
    var pitchBetweenCenters by remember { mutableStateOf("1.0") }
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
                    placeholder = { Text("0.50") },
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
                    placeholder = { Text("1.00") },
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
                    placeholder = { Text("0.01") },
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
                            placeholder = { Text("0.5") },
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
                            AntennaInfoRow("Внутрішній діаметр котушки:", "${String.format("%.2f", result.coilDiameterMm)} мм")
                            AntennaInfoRow("Висота котушки:", "${String.format("%.2f", result.coilHeightMm)} мм")
                            AntennaInfoRow("Діаметр дроту:", "${String.format("%.2f", result.wireDiameterMm)} мм")
                            AntennaInfoRow("Крок спіралі:", "${String.format("%.2f", result.pitchMm)} мм")

                            // Розділювач для електричних характеристик
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            AntennaInfoRow("Потужність передавача:", "${result.power} Вт")
                            AntennaInfoRow("EIRP (ефективна потужність):", "${String.format("%.4f", result.eirp)} Вт")
                            AntennaInfoRow("Коефіцієнт підсилення:", "${String.format("%.2f", result.gain)} дБi")
                            AntennaInfoRow("КПД антени:", "${String.format("%.2f", result.efficiency * 100)}%")
                            AntennaInfoRow("Вхідний опір:", "${String.format("%.0f", result.impedance)} Ом")
                            AntennaInfoRow("Смуга пропускання:", "${String.format("%.2f", result.bandwidth)} МГц")
                            AntennaInfoRow("Приблизна дальність дії:", "${String.format("%.0f", result.range)} м")

                            // Індуктивність винесемо окремо
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            AntennaInfoRow("Індуктивність:", "${String.format("%.4f", result.inductance)} мкГн")

                            // Новий розділ - аналіз товщини дроту
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Аналіз товщини дроту:",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            AntennaInfoRow("Глибина скін-ефекту:", "${String.format("%.4f", result.skinDepthMm)} мм")
                            AntennaInfoRow("Опір дроту:", "${String.format("%.4f", result.wireResistanceOhm)} Ом")
                            AntennaInfoRow("Опір випромінювання:", "${String.format("%.4f", result.radiationResistanceOhm)} Ом")
                            AntennaInfoRow("Добротність (Q):", "${String.format("%.4f", result.qualityFactor)}")
                            AntennaInfoRow("Використання скін-шару:", "${String.format("%.4f", result.skinEffectFactor * 100)}%")
                            AntennaInfoRow("Оцінка товщини дроту:", result.wireQuality)
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

    // ===== ВИПРАВЛЕНИЙ АНАЛІЗ ТОВЩИНИ ДРОТУ =====

    // Правильний розрахунок глибини скін-ефекту для міді
    val frequency = frequencyMHz * 1e6 // частота в Гц
    val mu0 = 4e-7 * PI // магнітна проникність вакууму H/м
    val sigma = 5.96e7 // провідність міді S/m при 20°C

    // Формула скін-ефекту: δ = √(2/(ωμσ))
    val skinDepth = sqrt(2.0 / (2 * PI * frequency * mu0 * sigma))
    val skinDepthMm = skinDepth * 1000 // переводимо в мм

    // Правильний розрахунок коефіцієнта використання
    val wireRadiusMm = wireDiameterMm / 2

    val skinEffectFactor = if (wireRadiusMm <= skinDepthMm) {
        // Тонкий дріт - весь переріз працює
        1.0
    } else {
        // Товстий дріт - тільки поверхневий шар
        // Ефективна площа = площа кільця товщиною skinDepth
        val outerArea = PI * wireRadiusMm.pow(2)
        val innerRadius = (wireRadiusMm - skinDepthMm).coerceAtLeast(0.0)
        val innerArea = PI * innerRadius.pow(2)
        val effectiveArea = outerArea - innerArea
        effectiveArea / outerArea
    }

    // Розрахунок опору дроту
    val rho = 1.72e-8 // питомий опір міді Ом⋅м при 20°C
    val wireArea = PI * (wireDiameterMm / 2000).pow(2) // повна площа в м²
    val effectiveArea = wireArea * skinEffectFactor

    val dcResistance = rho * actualTotalLength / effectiveArea

    // Проксимітні втрати (вплив сусідніх витків)
    val proximityFactor = when {
        pitchBetweenCentersMm <= wireDiameterMm * 1.05 -> 2.5 // майже торкаються
        pitchBetweenCentersMm <= wireDiameterMm * 1.2 -> 1.8  // дуже близько
        pitchBetweenCentersMm <= wireDiameterMm * 2.0 -> 1.4  // близько
        pitchBetweenCentersMm <= wireDiameterMm * 3.0 -> 1.2  // нормально
        else -> 1.1 // далеко
    }

    val totalWireResistance = dcResistance * proximityFactor

    // Опір випромінювання
    val lengthInWavelengths = actualTotalLength / wavelength
    val radiationResistance = when {
        lengthInWavelengths < 0.1 -> {
            790.0 * lengthInWavelengths.pow(2)
        }
        lengthInWavelengths < 0.5 -> {
            36.6 * lengthInWavelengths.pow(2)
        }
        else -> {
            73.1 * (lengthInWavelengths - 0.25).pow(2).coerceAtLeast(0.0) + 9.1
        }
    }

    // Ефективність
    val efficiency = (radiationResistance / (radiationResistance + totalWireResistance)).coerceIn(0.15, 0.98)

    // ПРАВИЛЬНА оцінка якості товщини дроту
    val skinRatio = wireRadiusMm / skinDepthMm
    val wireQuality = when {
        skinRatio < 0.8 -> "Тонкий (весь переріз працює ефективно)"
        skinRatio < 1.5 -> "Оптимальний (найкращий баланс)"
        skinRatio < 3.0 -> "Практичний (невеликі втрати матеріалу)"
        skinRatio < 6.0 -> "Товстий (частина матеріалу не використовується)"
        else -> "Надмірно товстий (неефективне використання матеріалу)"
    }

    // Розрахунок індуктивності
    val radiusMm = coilDiameterMm / 2
    val aspectRatio = if (coilHeightMm > 0) coilHeightMm / coilDiameterMm else 0.1

    val correctionFactor = when {
        aspectRatio < 0.2 -> 0.5  // плоска котушка
        aspectRatio < 1.0 -> 0.6 + aspectRatio * 0.3
        aspectRatio < 3.0 -> 0.9 + aspectRatio * 0.05
        else -> 1.0
    }

    // Формула Вілера для індуктивності (мкГн)
    val inductance = if (radiusMm > 0 && coilHeightMm > 0) {
        correctionFactor * (radiusMm * radiusMm * turns * turns) /
                (22.86 * radiusMm + 25.4 * coilHeightMm)
    } else {
        0.1 // мінімальна індуктивність
    }

    // Реалістична добротність
    val reactance = 2 * PI * frequencyMHz * 1e6 * inductance * 1e-6
    val qualityFactor = if (totalWireResistance > 0) {
        (reactance / totalWireResistance).coerceIn(5.0, 200.0)
    } else {
        50.0
    }

    // Коефіцієнт підсилення
    val circumferenceInWavelengths = (circumferenceMm / 1000) / wavelength
    val gain = when {
        turns <= 2 -> -2.0 + 4.0 * circumferenceInWavelengths
        turns <= 6 -> 0.0 + 6.0 * log10((turns * circumferenceInWavelengths * 10).coerceAtLeast(1.0))
        turns <= 12 -> 2.0 + 8.0 * log10((turns * circumferenceInWavelengths * 5).coerceAtLeast(1.0))
        else -> 4.0 + 10.0 * log10((turns * circumferenceInWavelengths * 2).coerceAtLeast(1.0))
    }.coerceIn(-5.0, 12.0)

    val gainLinear = 10.0.pow(gain / 10.0)

    // EIRP
    val eirp = powerW * gainLinear * efficiency

    // Вхідний опір
    val baseImpedance = when {
        circumferenceInWavelengths < 0.3 -> 20.0 + 40.0 * circumferenceInWavelengths
        circumferenceInWavelengths < 1.0 -> 35.0 + 25.0 * circumferenceInWavelengths
        else -> 60.0 + 15.0 * (circumferenceInWavelengths - 1.0)
    }

    val lossCorrection = 1.0 + (1.0 - efficiency) * 0.8
    val impedance = (baseImpedance * lossCorrection).coerceIn(15.0, 300.0)

    // Смуга пропускання
    val bandwidth = (frequencyMHz / qualityFactor).coerceAtLeast(0.5)

    // Дальність дії
    val rxSensitivity = when {
        frequencyMHz < 200 -> -115.0
        frequencyMHz < 500 -> -110.0
        frequencyMHz < 1000 -> -105.0
        else -> -100.0
    }

    val txPowerDbm = 10 * log10(powerW * 1000)
    val eirpDbm = txPowerDbm + gain + 10 * log10(efficiency)
    val linkMargin = 25.0 // більш консервативний запас
    val availableBudget = eirpDbm - rxSensitivity - linkMargin

    val range = if (availableBudget > 0) {
        val freeSpaceRange = lightSpeed / (4 * PI * frequencyMHz * 1e6) * 10.0.pow(availableBudget / 20.0)
        freeSpaceRange * 0.3 // реалістичний коефіцієнт для реальних умов
    } else {
        5.0
    }.coerceIn(5.0, 500.0)

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
        efficiency = efficiency,
        // Виправлені параметри
        skinDepthMm = skinDepthMm,
        wireResistanceOhm = totalWireResistance,
        radiationResistanceOhm = radiationResistance,
        qualityFactor = qualityFactor,
        skinEffectFactor = skinEffectFactor,
        wireQuality = wireQuality
    )
}

private fun buildRecommendations(result: AntennaInfo): String {
    return buildString {
        append("• Використовуйте мідний дріт діаметром ${result.wireDiameterMm} мм\n")
        append("• Основа для намотування: труба діаметром ${String.format("%.2f", result.coilDiameterMm)} мм\n")
        append("• Висота котушки: ${String.format("%.2f", result.coilHeightMm)} мм\n")
        append("• Намотайте ${result.turns} витків з кроком ${String.format("%.2f", result.pitchMm)} мм\n")

        // Виправлені рекомендації по товщині дроту
        append("\n🔧 Аналіз товщини дроту:\n")
        append("• Глибина скін-ефекту для ${result.frequency} МГц: ${String.format("%.4f", result.skinDepthMm)} мм\n")

        val skinRatio = (result.wireDiameterMm / 2) / result.skinDepthMm
        when {
            skinRatio < 0.8 -> {
                append("• Дріт тонкий - весь переріз (${String.format("%.3f", result.skinEffectFactor * 100)}%) ефективно використовується\n")
                append("• Можна збільшити товщину до ${String.format("%.3f", result.skinDepthMm * 2)} мм для зменшення опору\n")
            }
            skinRatio < 1.5 -> {
                append("• ✅ Оптимальна товщина дроту для ${result.frequency} МГц!\n")
                append("• Відмінний баланс між ефективністю та вартістю матеріалу\n")
            }
            skinRatio < 3.0 -> {
                append("• Практична товщина з невеликими надлишками\n")
                append("• ${String.format("%.2f", result.skinEffectFactor * 100)}% перерізу активно працює\n")
            }
            skinRatio < 6.0 -> {
                val recommendedDiameter = result.skinDepthMm * 3
                append("• ⚠️ Дріт товстіший за потрібний\n")
                append("• Рекомендована товщина: ~${String.format("%.2f", recommendedDiameter)} мм\n")
                append("• Можна заощадити матеріал без втрати ефективності\n")
            }
            else -> {
                val recommendedDiameter = result.skinDepthMm * 2.5
                append("• ❌ Надмірно товстий дріт!\n")
                append("• Тільки ${String.format("%.2f", result.skinEffectFactor * 100)}% перерізу працює\n")
                append("• Оптимальна товщина: ${String.format("%.4f", recommendedDiameter)} мм\n")
            }
        }

        // Аналіз ефективності
        when {
            result.efficiency < 0.5 -> {
                append("• ❌ Низька ефективність (${String.format("%.2f", result.efficiency * 100)}%) - великі втрати в дроті\n")
                append("• Збільште товщину дроту або зменште кількість витків\n")
            }
            result.efficiency < 0.7 -> {
                append("• ⚠️ Помірна ефективність (${String.format("%.2f", result.efficiency * 100)}%) - можна покращити\n")
            }
            result.efficiency < 0.85 -> {
                append("• ✅ Хороша ефективність (${String.format("%.2f", result.efficiency * 100)}%)\n")
            }
            else -> {
                append("• 🌟 Відмінна ефективність (${String.format("%.2f", result.efficiency * 100)}%)!\n")
            }
        }

        // Аналіз добротності
        if (result.qualityFactor < 20) {
            append("• Низька добротність (Q=${String.format("%.2f", result.qualityFactor)}) - широкосмугова робота\n")
        } else if (result.qualityFactor > 100) {
            append("• Висока добротність (Q=${String.format("%.2f", result.qualityFactor)}) - вузькосмугова, потребує точного налаштування\n")
        }

        append("\n📡 Загальні поради:\n")
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