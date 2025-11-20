
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
    val power: Double
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiralAntennaCalculator(modifier: Modifier = Modifier) {
    var frequency by remember { mutableStateOf("433.0") }
    var wireDiameter by remember { mutableStateOf("1.0") }
    var power by remember { mutableStateOf("10.0") }
    var calculationResult by remember { mutableStateOf<AntennaInfo?>(null) }
    var errorMessage by remember { mutableStateOf("") }

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

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка розрахунку
        Button(
            onClick = {
                try {
                    val result = calculateSpiralAntenna(
                        frequency.toDouble(),
                        wireDiameter.toDouble(),
                        power.toDouble()
                    )
                    calculationResult = result
                    errorMessage = ""
                } catch (e: Exception) {
                    errorMessage = "Помилка: ${e.message}"
                    calculationResult = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Розрахувати антену")
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
                            AntennaInfoRow("Довжина хвилі:", "${String.format("%.2f", result.wavelength)} м")
                            AntennaInfoRow("Загальна довжина дроту:", "${String.format("%.2f", result.totalLength)} м")
                            AntennaInfoRow("Кількість витків:", "${result.turns}")
                            AntennaInfoRow("Діаметр котушки:", "${String.format("%.2f", result.coilDiameter)} см")
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
                        text = "• Використовуйте мідний дріт для кращої провідності\n" +
                                "• Намотайте антену на циліндричній основі (труба, стержень)\n" +
                                "• Забезпечте рівномірний крок між витками\n" +
                                "• Підключіть центральний провідник до одного кінця, екран - до іншого\n" +
                                "• Для 433 МГц оптимальна поляризація - вертикальна\n" +
                                "• Розташовуйте антену якомога вище над землею",
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

private fun calculateSpiralAntenna(frequencyMHz: Double, wireDiameterMm: Double, powerW: Double): AntennaInfo {
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

    // Розрахунок оптимального діаметру котушки (приблизно 1/10 довжини хвилі)
    val coilDiameter = (wavelength / 10) * 100 // в сантиметрах

    // Розрахунок кількості витків (базується на співвідношенні довжини до діаметру)
    val circumference = PI * (coilDiameter / 100) // окружність в метрах
    val estimatedTurns = (totalLength / circumference).toInt().coerceAtLeast(3)

    // Уточнення кількості витків для оптимізації
    val turns = when {
        frequencyMHz < 100 -> estimatedTurns.coerceAtLeast(5)
        frequencyMHz < 500 -> estimatedTurns.coerceAtLeast(3)
        else -> estimatedTurns.coerceAtLeast(2)
    }

    // Розрахунок кроку спіралі
    val totalHeight = totalLength / turns * 0.8 // 80% від загальної довжини для висоти
    val pitch = (totalHeight * 1000) / turns // в міліметрах

    // Розрахунок індуктивності (приблизна формула для багатовиткової котушки)
    val radiusCm = coilDiameter / 2
    val lengthCm = totalHeight * 100
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
        power = powerW
    )
}