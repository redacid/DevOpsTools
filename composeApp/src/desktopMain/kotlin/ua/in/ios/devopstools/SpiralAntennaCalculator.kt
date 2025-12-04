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
    val coilDiameterMm: Double,     // in mm
    val wireDiameterMm: Double,     // in mm
    val pitchMm: Double,            // in mm
    val inductance: Double,
    val gain: Double,
    val power: Double,
    val coilHeightMm: Double,       // in mm
    val eirp: Double,
    val range: Double,              // in meters
    val impedance: Double,
    val bandwidth: Double,
    val efficiency: Double,
    // New parameters for wire thickness analysis
    val skinDepthMm: Double,        // skin depth in mm
    val wireResistanceOhm: Double,  // wire resistance in Ohms
    val radiationResistanceOhm: Double, // radiation resistance in Ohms
    val qualityFactor: Double,      // Q-factor
    val skinEffectFactor: Double,   // skin effect utilization factor
    val wireQuality: String         // assessment of wire thickness
)

enum class WavelengthFraction(val description: String, val factor: Double) {
    FULL_WAVE("1λ (Full Wave)", 1.0),
    HALF_WAVE("λ/2 (Half Wave)", 0.5),
    THIRD_WAVE("λ/3 (Third Wave)", 1.0 / 3.0),
    QUARTER_WAVE("λ/4 (Quarter Wave)", 0.25)
}

enum class CalculationMode {
    FIXED_DIAMETER, // Fixed diameter
    FIXED_TURNS     // Fixed number of turns
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpiralAntennaCalculator(modifier: Modifier = Modifier) {
    var frequency by remember { mutableStateOf("433.92") }
    var wireDiameter by remember { mutableStateOf("0.75") }
    var power by remember { mutableStateOf("0.01") }
    var wavelengthFraction by remember { mutableStateOf(WavelengthFraction.QUARTER_WAVE) }
    var calculationMode by remember { mutableStateOf(CalculationMode.FIXED_DIAMETER) }
    var fixedCoilDiameter by remember { mutableStateOf("5.0") }
    var fixedTurns by remember { mutableStateOf("10") }
    var pitchBetweenCenters by remember { mutableStateOf("3.0") }
    var calculationResult by remember { mutableStateOf<AntennaInfo?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    // Automatic calculation on parameter change
    LaunchedEffect(frequency, wireDiameter, power, wavelengthFraction, calculationMode, fixedCoilDiameter, fixedTurns, pitchBetweenCenters) {
        if (frequency.isNotBlank() && wireDiameter.isNotBlank() && power.isNotBlank() && pitchBetweenCenters.isNotBlank()) {
            try {
                val result = calculateSpiralAntenna(
                    frequency.toDouble(),
                    wireDiameter.toDouble(),
                    power.toDouble(),
                    wavelengthFraction,
                    calculationMode,
                    if (calculationMode == CalculationMode.FIXED_DIAMETER) fixedCoilDiameter.toDoubleOrNull() else null,
                    if (calculationMode == CalculationMode.FIXED_TURNS) fixedTurns.toIntOrNull() else null,
                    pitchBetweenCenters.toDouble()
                )
                calculationResult = result
                errorMessage = ""
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
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
            text = "Spiral Antenna Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        Text(
            text = "Calculation of spiral antenna parameters for receivers and transmitters (e.g., garage doors)",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Main Parameters
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Main Parameters",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Standard Frequencies
                val standardFrequencies = listOf(
                    "ISM Europe (433 MHz)" to "433.92",
                    "ISM Europe (868 MHz)" to "868.3",
                    "ISM US (315 MHz)" to "315.0",
                    "ISM US (915 MHz)" to "915.0",
                    "Wi-Fi / BT (2.4 GHz)" to "2440.0",
                    "Wi-Fi 5G (Ch 36)" to "5180.0",
                    "Wi-Fi 5G (Ch 100)" to "5500.0",
                    "Wi-Fi 5G (Ch 149)" to "5745.0",
                    "GPS L1" to "1575.42",
                    "LoRa EU" to "868.0",
                    "LoRa US" to "915.0"
                )
                var freqExpanded by remember { mutableStateOf(false) }

                ExposedDropdownMenuBox(
                    expanded = freqExpanded,
                    onExpandedChange = { freqExpanded = !freqExpanded }
                ) {
                    OutlinedTextField(
                        value = frequency,
                        onValueChange = { frequency = it },
                        label = { Text("Frequency (MHz)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = freqExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        supportingText = { Text("Select a standard or enter a custom value") }
                    )
                    ExposedDropdownMenu(
                        expanded = freqExpanded,
                        onDismissRequest = { freqExpanded = false }
                    ) {
                        standardFrequencies.forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text("${selectionOption.first} - ${selectionOption.second} MHz") },
                                onClick = {
                                    frequency = selectionOption.second
                                    freqExpanded = false
                                }
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Radiator Length Dropdown
                var lengthExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = lengthExpanded,
                    onExpandedChange = { lengthExpanded = !lengthExpanded }
                ) {
                    OutlinedTextField(
                        value = wavelengthFraction.description,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Radiator Length") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = lengthExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        supportingText = { Text("Electrical length of the antenna wire") }
                    )
                    ExposedDropdownMenu(
                        expanded = lengthExpanded,
                        onDismissRequest = { lengthExpanded = false }
                    ) {
                        WavelengthFraction.values().forEach { selectionOption ->
                            DropdownMenuItem(
                                text = { Text(selectionOption.description) },
                                onClick = {
                                    wavelengthFraction = selectionOption
                                    lengthExpanded = false
                                }
                            )
                        }
                    }
                }


                Spacer(modifier = Modifier.height(16.dp))

                // Wire diameter input field
                OutlinedTextField(
                    value = wireDiameter,
                    onValueChange = {
                        wireDiameter = it
                        errorMessage = ""
                    },
                    label = { Text("Wire Diameter (mm)") },
                    placeholder = { Text("0.75") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Typical values: 0.3-2.0 mm") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Pitch between turn centers input field
                OutlinedTextField(
                    value = pitchBetweenCenters,
                    onValueChange = {
                        pitchBetweenCenters = it
                        errorMessage = ""
                    },
                    label = { Text("Pitch between turn centers (mm)") },
                    placeholder = { Text("3.00") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("Distance between the centers of adjacent turns. Minimum = wire diameter") }
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Power input field
                OutlinedTextField(
                    value = power,
                    onValueChange = {
                        power = it
                        errorMessage = ""
                    },
                    label = { Text("Transmitter Power (W)") },
                    placeholder = { Text("0.01") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    supportingText = { Text("For garage doors, typically 0.01-10 W") }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calculation Mode
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Calculation Mode",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Radio buttons for mode selection
                Column {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = calculationMode == CalculationMode.FIXED_DIAMETER,
                            onClick = { calculationMode = CalculationMode.FIXED_DIAMETER }
                        )
                        Text(
                            text = "Set Coil Diameter",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }

                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        RadioButton(
                            selected = calculationMode == CalculationMode.FIXED_TURNS,
                            onClick = { calculationMode = CalculationMode.FIXED_TURNS }
                        )
                        Text(
                            text = "Set Number of Turns",
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }

                // Additional fields depending on the mode
                when (calculationMode) {
                    CalculationMode.FIXED_DIAMETER -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fixedCoilDiameter,
                            onValueChange = { fixedCoilDiameter = it },
                            label = { Text("Inner Coil Diameter (mm)") },
                            placeholder = { Text("5.0") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            supportingText = { Text("Diameter of the winding form") }
                        )
                    }
                    CalculationMode.FIXED_TURNS -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        OutlinedTextField(
                            value = fixedTurns,
                            onValueChange = { fixedTurns = it },
                            label = { Text("Number of Turns") },
                            placeholder = { Text("5") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            supportingText = { Text("Recommended: 3-15 turns") }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Error display
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

        // Results display
        calculationResult?.let { result ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Calculation Results",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SelectionContainer {
                        Column {
                            AntennaInfoRow("Frequency:", "${result.frequency} MHz")
                            AntennaInfoRow("Wavelength:", "${String.format("%.0f", result.wavelength * 1000)} mm")
                            AntennaInfoRow("Total Wire Length:", "${String.format("%.0f", result.totalLength * 1000)} mm")
                            AntennaInfoRow("Number of Turns:", "${result.turns}")
                            AntennaInfoRow("Inner Coil Diameter:", "${String.format("%.2f", result.coilDiameterMm)} mm")
                            AntennaInfoRow("Coil Height:", "${String.format("%.2f", result.coilHeightMm)} mm")
                            AntennaInfoRow("Wire Diameter:", "${String.format("%.2f", result.wireDiameterMm)} mm")
                            AntennaInfoRow("Spiral Pitch:", "${String.format("%.2f", result.pitchMm)} mm")

                            // Divider for electrical characteristics
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))

                            AntennaInfoRow("Transmitter Power:", "${result.power} W")
                            AntennaInfoRow("EIRP (Effective Power):", "${String.format("%.4f", result.eirp)} W")
                            AntennaInfoRow("Gain:", "${String.format("%.2f", result.gain)} dBi")
                            AntennaInfoRow("Antenna Efficiency:", "${String.format("%.2f", result.efficiency * 100)}%")
                            AntennaInfoRow("Input Impedance:", "${String.format("%.0f", result.impedance)} Ohm")
                            AntennaInfoRow("Bandwidth:", "${String.format("%.2f", result.bandwidth)} MHz")
                            AntennaInfoRow("Approximate Range:", "${String.format("%.0f", result.range)} m")

                            // Inductance separately
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            AntennaInfoRow("Inductance:", "${String.format("%.4f", result.inductance)} µH")

                            // New section - wire thickness analysis
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Wire Thickness Analysis:",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )

                            AntennaInfoRow("Skin Depth:", "${String.format("%.4f", result.skinDepthMm)} mm")
                            AntennaInfoRow("Wire Resistance:", "${String.format("%.4f", result.wireResistanceOhm)} Ohm")
                            AntennaInfoRow("Radiation Resistance:", "${String.format("%.4f", result.radiationResistanceOhm)} Ohm")
                            AntennaInfoRow("Quality Factor (Q):", "${String.format("%.4f", result.qualityFactor)}")
                            AntennaInfoRow("Skin Layer Usage:", "${String.format("%.4f", result.skinEffectFactor * 100)}%")
                            AntennaInfoRow("Wire Thickness Assessment:", result.wireQuality)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional information
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Manufacturing Recommendations",
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
    wavelengthFraction: WavelengthFraction,
    mode: CalculationMode,
    fixedCoilDiameterMm: Double? = null,
    fixedTurns: Int? = null,
    pitchBetweenCentersMm: Double = 1.0
): AntennaInfo {
    // Input validation
    if (frequencyMHz <= 0) throw IllegalArgumentException("Frequency must be greater than 0")
    if (wireDiameterMm <= 0) throw IllegalArgumentException("Wire diameter must be greater than 0")
    if (powerW <= 0) throw IllegalArgumentException("Power must be greater than 0")
    if (pitchBetweenCentersMm < wireDiameterMm) throw IllegalArgumentException("Pitch between turns cannot be less than the wire diameter")

    // Speed of light (m/s)
    val lightSpeed = 299_792_458.0

    // Wavelength calculation
    val wavelength = lightSpeed / (frequencyMHz * 1_000_000)

    // Calculate total electrical length based on the selected fraction
    val totalLength = wavelength * wavelengthFraction.factor

    // Parameter calculation depending on the mode
    val (coilDiameterMm, turns) = when (mode) {
        CalculationMode.FIXED_DIAMETER -> {
            if (fixedCoilDiameterMm == null || fixedCoilDiameterMm <= 0) {
                throw IllegalArgumentException("Coil diameter must be greater than 0")
            }
            // Calculate number of turns for a given diameter
            val circumferenceMm = PI * fixedCoilDiameterMm
            val calculatedTurns = (totalLength * 1000 / circumferenceMm).toInt().coerceIn(2, 30)
            Pair(fixedCoilDiameterMm, calculatedTurns)
        }
        CalculationMode.FIXED_TURNS -> {
            if (fixedTurns == null || fixedTurns <= 0) {
                throw IllegalArgumentException("Number of turns must be greater than 0")
            }
            // Calculate diameter for a given number of turns
            val circumferenceMm = totalLength * 1000 / fixedTurns
            val calculatedDiameterMm = circumferenceMm / PI
            Pair(calculatedDiameterMm, fixedTurns)
        }
    }

    // Coil height calculation
    val coilHeightMm = (turns - 1) * pitchBetweenCentersMm // height = (turns - 1) × pitch

    // Actual wire length calculation
    val circumferenceMm = PI * coilDiameterMm
    val wireLengthPerTurn = sqrt(circumferenceMm.pow(2) + pitchBetweenCentersMm.pow(2))
    val actualTotalLength = wireLengthPerTurn * turns / 1000 // in meters

    // ===== CORRECTED WIRE THICKNESS ANALYSIS =====

    // Correct calculation of skin depth for copper
    val frequency = frequencyMHz * 1e6 // frequency in Hz
    val mu0 = 4e-7 * PI // magnetic permeability of vacuum H/m
    val sigma = 5.96e7 // conductivity of copper S/m at 20°C

    // Skin effect formula: δ = √(2/(ωμσ))
    val skinDepth = sqrt(2.0 / (2 * PI * frequency * mu0 * sigma))
    val skinDepthMm = skinDepth * 1000 // convert to mm

    // Correct calculation of utilization factor
    val wireRadiusMm = wireDiameterMm / 2

    val skinEffectFactor = if (wireRadiusMm <= skinDepthMm) {
        // Thin wire - entire cross-section is effective
        1.0
    } else {
        // Thick wire - only the surface layer is effective
        // Effective area = area of a ring with thickness skinDepth
        val outerArea = PI * wireRadiusMm.pow(2)
        val innerRadius = (wireRadiusMm - skinDepthMm).coerceAtLeast(0.0)
        val innerArea = PI * innerRadius.pow(2)
        val effectiveArea = outerArea - innerArea
        effectiveArea / outerArea
    }

    // Wire resistance calculation
    val rho = 1.72e-8 // resistivity of copper Ohm⋅m at 20°C
    val wireArea = PI * (wireDiameterMm / 2000).pow(2) // total area in m²
    val effectiveArea = wireArea * skinEffectFactor

    val dcResistance = rho * actualTotalLength / effectiveArea

    // Proximity effect losses (influence of adjacent turns)
    val proximityFactor = when {
        pitchBetweenCentersMm <= wireDiameterMm * 1.05 -> 2.5 // almost touching
        pitchBetweenCentersMm <= wireDiameterMm * 1.2 -> 1.8  // very close
        pitchBetweenCentersMm <= wireDiameterMm * 2.0 -> 1.4  // close
        pitchBetweenCentersMm <= wireDiameterMm * 3.0 -> 1.2  // normal
        else -> 1.1 // far
    }

    val totalWireResistance = dcResistance * proximityFactor

    // Radiation resistance
    val lengthInWavelengths = actualTotalLength / wavelength
    val radiationResistance = when {
        lengthInWavelengths < 0.1 -> 790.0 * lengthInWavelengths.pow(2)
        lengthInWavelengths < 0.5 -> 36.6 * lengthInWavelengths.pow(2)
        else -> 73.1 * (lengthInWavelengths - 0.25).pow(2).coerceAtLeast(0.0) + 9.1
    }

    // Efficiency
    val efficiency = (radiationResistance / (radiationResistance + totalWireResistance)).coerceIn(0.15, 0.98)

    // CORRECT assessment of wire thickness quality
    val skinRatio = wireRadiusMm / skinDepthMm
    val wireQuality = when {
        skinRatio < 0.8 -> "Thin (entire cross-section works effectively)"
        skinRatio < 1.5 -> "Optimal (best balance)"
        skinRatio < 3.0 -> "Practical (small material losses)"
        skinRatio < 6.0 -> "Thick (part of the material is not used)"
        else -> "Excessively thick (inefficient use of material)"
    }

    // Inductance calculation
    val radiusMm = coilDiameterMm / 2
    val aspectRatio = if (coilHeightMm > 0) coilHeightMm / coilDiameterMm else 0.1

    val correctionFactor = when {
        aspectRatio < 0.2 -> 0.5  // flat coil
        aspectRatio < 1.0 -> 0.6 + aspectRatio * 0.3
        aspectRatio < 3.0 -> 0.9 + aspectRatio * 0.05
        else -> 1.0
    }

    // Wheeler's formula for inductance (µH)
    val inductance = if (radiusMm > 0 && coilHeightMm > 0) {
        correctionFactor * (radiusMm * radiusMm * turns * turns) /
                (22.86 * radiusMm + 25.4 * coilHeightMm)
    } else {
        0.1 // minimal inductance
    }

    // Realistic Q-factor
    val reactance = 2 * PI * frequencyMHz * 1e6 * inductance * 1e-6
    val qualityFactor = if (totalWireResistance > 0) {
        (reactance / totalWireResistance).coerceIn(5.0, 200.0)
    } else {
        50.0
    }

    // Gain
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

    // Input impedance
    val baseImpedance = when {
        circumferenceInWavelengths < 0.3 -> 20.0 + 40.0 * circumferenceInWavelengths
        circumferenceInWavelengths < 1.0 -> 35.0 + 25.0 * circumferenceInWavelengths
        else -> 60.0 + 15.0 * (circumferenceInWavelengths - 1.0)
    }

    val lossCorrection = 1.0 + (1.0 - efficiency) * 0.8
    val impedance = (baseImpedance * lossCorrection).coerceIn(15.0, 300.0)

    // Bandwidth
    val bandwidth = (frequencyMHz / qualityFactor).coerceAtLeast(0.5)

    // Range
    val rxSensitivity = when {
        frequencyMHz < 200 -> -115.0
        frequencyMHz < 500 -> -110.0
        frequencyMHz < 1000 -> -105.0
        else -> -100.0
    }

    val txPowerDbm = 10 * log10(powerW * 1000)
    val eirpDbm = txPowerDbm + gain + 10 * log10(efficiency)
    val linkMargin = 25.0 // more conservative margin
    val availableBudget = eirpDbm - rxSensitivity - linkMargin

    val range = if (availableBudget > 0) {
        val freeSpaceRange = lightSpeed / (4 * PI * frequencyMHz * 1e6) * 10.0.pow(availableBudget / 20.0)
        freeSpaceRange * 0.3 // realistic factor for real-world conditions
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
        // Corrected parameters
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
        append("• Use copper wire with a diameter of ${result.wireDiameterMm} mm\n")
        append("• Winding form: a tube with a diameter of ${String.format("%.2f", result.coilDiameterMm)} mm\n")
        append("• Coil height: ${String.format("%.2f", result.coilHeightMm)} mm\n")
        append("• Wind ${result.turns} turns with a pitch of ${String.format("%.2f", result.pitchMm)} mm\n")

        // Corrected recommendations for wire thickness
        append("\n🔧 Wire Thickness Analysis:\n")
        append("• Skin depth for ${result.frequency} MHz: ${String.format("%.4f", result.skinDepthMm)} mm\n")

        val skinRatio = (result.wireDiameterMm / 2) / result.skinDepthMm
        when {
            skinRatio < 0.8 -> {
                append("• Wire is thin - the entire cross-section (${String.format("%.3f", result.skinEffectFactor * 100)}%) is used effectively\n")
                append("• You can increase the thickness to ${String.format("%.3f", result.skinDepthMm * 2)} mm to reduce resistance\n")
            }
            skinRatio < 1.5 -> {
                append("• ✅ Optimal wire thickness for ${result.frequency} MHz!\n")
                append("• Excellent balance between efficiency and material cost\n")
            }
            skinRatio < 3.0 -> {
                append("• Practical thickness with small excess\n")
                append("• ${String.format("%.2f", result.skinEffectFactor * 100)}% of the cross-section is actively working\n")
            }
            skinRatio < 6.0 -> {
                val recommendedDiameter = result.skinDepthMm * 3
                append("• ⚠️ Wire is thicker than necessary\n")
                append("• Recommended thickness: ~${String.format("%.2f", recommendedDiameter)} mm\n")
                append("• You can save material without losing efficiency\n")
            }
            else -> {
                val recommendedDiameter = result.skinDepthMm * 2.5
                append("• ❌ Excessively thick wire!\n")
                append("• Only ${String.format("%.2f", result.skinEffectFactor * 100)}% of the cross-section is working\n")
                append("• Optimal thickness: ${String.format("%.4f", recommendedDiameter)} mm\n")
            }
        }

        // Efficiency analysis
        when {
            result.efficiency < 0.5 -> {
                append("• ❌ Low efficiency (${String.format("%.2f", result.efficiency * 100)}%) - high losses in the wire\n")
                append("• Increase the wire thickness or decrease the number of turns\n")
            }
            result.efficiency < 0.7 -> {
                append("• ⚠️ Moderate efficiency (${String.format("%.2f", result.efficiency * 100)}%) - can be improved\n")
            }
            result.efficiency < 0.85 -> {
                append("• ✅ Good efficiency (${String.format("%.2f", result.efficiency * 100)}%)\n")
            }
            else -> {
                append("• 🌟 Excellent efficiency (${String.format("%.2f", result.efficiency * 100)}%)!\n")
            }
        }

        // Q-factor analysis
        if (result.qualityFactor < 20) {
            append("• Low Q-factor (Q=${String.format("%.2f", result.qualityFactor)}) - broadband operation\n")
        } else if (result.qualityFactor > 100) {
            append("• High Q-factor (Q=${String.format("%.2f", result.qualityFactor)}) - narrowband, requires precise tuning\n")
        }

        append("\n📡 General Advice:\n")
        append("• Connect the center conductor to one end, and the ground/shield to the other if applicable\n")
        append("• For ${result.frequency} MHz, vertical polarization is often optimal\n")
        append("• Position the antenna as high as possible and clear of obstructions\n")

        // Recommendations based on power
        when {
            result.power <= 1 -> append("• Low power: suitable for short-range applications\n")
            result.power <= 10 -> append("• Medium power: optimal for garage doors and remote controls\n")
            result.power > 10 -> append("• High power: ensure good matching (SWR < 1.5) to protect the transmitter\n")
        }

        if (result.frequency > 433.91 && result.frequency < 433.93) {
            append("• For 433 MHz ISM band: comply with local regulatory power limits")
        }
    }
}