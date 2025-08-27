
package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.pow

data class SubnetInfo(
    val networkAddress: String,
    val broadcastAddress: String,
    val firstHost: String,
    val lastHost: String,
    val subnetMask: String,
    val wildcardMask: String,
    val numberOfHosts: Long,
    val cidrNotation: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubnetCalculator(modifier: Modifier = Modifier) {
    var ipAddress by remember { mutableStateOf("192.168.1.1") }
    var subnetMask by remember { mutableStateOf("24") }
    var calculationResult by remember { mutableStateOf<SubnetInfo?>(null) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "IP Subnet Calculator",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Поле для введення IP адреси
        OutlinedTextField(
            value = ipAddress,
            onValueChange = {
                ipAddress = it
                errorMessage = ""
            },
            label = { Text("IP Address") },
            placeholder = { Text("192.168.1.1") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Поле для введення маски підмережі
        OutlinedTextField(
            value = subnetMask,
            onValueChange = {
                subnetMask = it
                errorMessage = ""
            },
            label = { Text("Subnet Mask (CIDR)") },
            placeholder = { Text("24") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Кнопка розрахунку
        Button(
            onClick = {
                try {
                    val result = calculateSubnet(ipAddress, subnetMask.toInt())
                    calculationResult = result
                    errorMessage = ""
                } catch (e: Exception) {
                    errorMessage = "Error: ${e.message}"
                    calculationResult = null
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Calculate")
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
                        text = "Result",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    SelectionContainer {
                        Column {
                            SubnetInfoRow("CIDR:", result.cidrNotation)
                            SubnetInfoRow("Network:", result.networkAddress)
                            SubnetInfoRow("Broadcast:", result.broadcastAddress)
                            SubnetInfoRow("First host:", result.firstHost)
                            SubnetInfoRow("Last host:", result.lastHost)
                            SubnetInfoRow("Mask:", result.subnetMask)
                            SubnetInfoRow("Wildcard:", result.wildcardMask)
                            SubnetInfoRow("Host count:", result.numberOfHosts.toString())
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SubnetInfoRow(label: String, value: String) {
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

private fun calculateSubnet(ipAddress: String, cidr: Int): SubnetInfo {
    // Валідація CIDR
    if (cidr < 0 || cidr > 32) {
        throw IllegalArgumentException("Cidr should be between 0 and 32")
    }

    // Парсинг IP адреси
    val ipParts = ipAddress.split(".")
    if (ipParts.size != 4) {
        throw IllegalArgumentException("Incorrect IP address format")
    }

    val ipBytes = ipParts.map { part ->
        val byte = part.toIntOrNull()
        if (byte == null || byte < 0 || byte > 255) {
            throw IllegalArgumentException("Incorrect Octet IP Address: $part")
        }
        byte
    }

    // Розрахунок маски підмережі
    val subnetMaskLong = (0xFFFFFFFFL shl (32 - cidr)) and 0xFFFFFFFFL
    val subnetMaskBytes = listOf(
        (subnetMaskLong shr 24).toInt() and 0xFF,
        (subnetMaskLong shr 16).toInt() and 0xFF,
        (subnetMaskLong shr 8).toInt() and 0xFF,
        subnetMaskLong.toInt() and 0xFF
    )

    // Розрахунок wildcard маски
    val wildcardMaskBytes = subnetMaskBytes.map { 255 - it }

    // Розрахунок мережевої адреси
    val networkBytes = ipBytes.zip(subnetMaskBytes) { ip, mask -> ip and mask }

    // Розрахунок широкомовної адреси
    val broadcastBytes = networkBytes.zip(wildcardMaskBytes) { network, wildcard -> network or wildcard }

    // Розрахунок першого та останнього хоста
    val firstHostBytes = networkBytes.toMutableList()
    firstHostBytes[3] = firstHostBytes[3] + 1

    val lastHostBytes = broadcastBytes.toMutableList()
    lastHostBytes[3] = lastHostBytes[3] - 1

    // Кількість хостів
    val numberOfHosts = 2.0.pow(32 - cidr).toLong() - 2

    return SubnetInfo(
        networkAddress = networkBytes.joinToString("."),
        broadcastAddress = broadcastBytes.joinToString("."),
        firstHost = firstHostBytes.joinToString("."),
        lastHost = lastHostBytes.joinToString("."),
        subnetMask = subnetMaskBytes.joinToString("."),
        wildcardMask = wildcardMaskBytes.joinToString("."),
        numberOfHosts = numberOfHosts,
        cidrNotation = "${networkBytes.joinToString(".")}/$cidr"
    )
}