package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.Socket
import java.util.regex.Pattern

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoisLookup(modifier: Modifier = Modifier) {
    var domain by remember { mutableStateOf("google.com") }
    var whoisResult by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    fun performWhoisLookup() {
        if (domain.isBlank()) {
            errorMessage = "Please enter a domain or IP address"
            return
        }

        coroutineScope.launch {
            isLoading = true
            errorMessage = ""
            whoisResult = ""

            try {
                val result = withContext(Dispatchers.IO) {
                    performWhoisQuery(domain.trim())
                }
                whoisResult = result
            } catch (e: Exception) {
                errorMessage = "Error when performing whois request: ${e.message}"
            } finally {
                isLoading = false
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
            text = "WHOIS Lookup",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        // Поле для введення домену або IP
        OutlinedTextField(
            value = domain,
            onValueChange = {
                domain = it
                errorMessage = ""
            },
            label = { Text("Domain or IP address") },
            placeholder = { Text("example.com або 8.8.8.8") },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    performWhoisLookup()
                }
            ),
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Кнопка пошуку
        Button(
            onClick = { performWhoisLookup() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("A request is performed...")
            } else {
                Text("Whois request")
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
        if (whoisResult.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Whois information for: $domain",
                            style = MaterialTheme.typography.titleMedium
                        )

                        TextButton(
                            onClick = {
                                // Тут можна додати функцію копіювання в буфер обміну
                                // Поки що просто очищуємо результат
                                whoisResult = ""
                            }
                        ) {
                            Text("Clean")
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SelectionContainer {
                        Text(
                            text = whoisResult,
                            fontFamily = FontFamily.Monospace,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private suspend fun performWhoisQuery(query: String): String {
    val whoisServer = getWhoisServer(query)

    return try {
        val socket = Socket(whoisServer, 43)
        val writer = socket.getOutputStream()
        val reader = BufferedReader(InputStreamReader(socket.getInputStream()))

        // Відправляємо запит
        writer.write("$query\r\n".toByteArray())
        writer.flush()

        // Читаємо відповідь
        val response = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            response.append(line).append("\n")
        }

        socket.close()
        response.toString()

    } catch (e: Exception) {
        throw Exception("Failed to connect to whois server $whoisServer: ${e.message}")
    }
}

private fun getWhoisServer(query: String): String {
    // Перевіряємо чи це IP адреса
    if (isIpAddress(query)) {
        return "whois.arin.net"
    }

    // Визначаємо WHOIS сервер на основі домену верхнього рівня
    val tld = query.substringAfterLast('.').lowercase()

    return when (tld) {
        "com", "net" -> "whois.verisign-grs.com"
        "org" -> "whois.pir.org"
        "edu" -> "whois.educause.edu"
        "gov" -> "whois.dotgov.gov"
        "mil" -> "whois.nic.mil"
        "int" -> "whois.iana.org"
        "biz" -> "whois.neulevel.biz"
        "info" -> "whois.afilias.net"
        "name" -> "whois.nic.name"
        "pro" -> "whois.registrypro.pro"
        "aero" -> "whois.aero"
        "coop" -> "whois.nic.coop"
        "museum" -> "whois.museum"
        "ua" -> "whois.ua"
        "ru" -> "whois.ripn.net"
        "uk" -> "whois.nic.uk"
        "de" -> "whois.denic.de"
        "fr" -> "whois.nic.fr"
        "it" -> "whois.nic.it"
        "nl" -> "whois.domain-registry.nl"
        "be" -> "whois.dns.be"
        "ch" -> "whois.nic.ch"
        "at" -> "whois.nic.at"
        "pl" -> "whois.dns.pl"
        "cz" -> "whois.nic.cz"
        "sk" -> "whois.sk-nic.sk"
        "hu" -> "whois.nic.hu"
        "ro" -> "whois.rotld.ro"
        "bg" -> "whois.register.bg"
        "si" -> "whois.arnes.si"
        "hr" -> "whois.dns.hr"
        "ee" -> "whois.tld.ee"
        "lv" -> "whois.nic.lv"
        "lt" -> "whois.domreg.lt"
        "fi" -> "whois.ficora.fi"
        "se" -> "whois.iis.se"
        "no" -> "whois.norid.no"
        "dk" -> "whois.dk-hostmaster.dk"
        "is" -> "whois.isnic.is"
        "ie" -> "whois.domainregistry.ie"
        "es" -> "whois.nic.es"
        "pt" -> "whois.dns.pt"
        "gr" -> "whois.ics.forth.gr"
        "tr" -> "whois.nic.tr"
        "il" -> "whois.isoc.org.il"
        "jp" -> "whois.jprs.jp"
        "kr" -> "whois.nic.or.kr"
        "cn" -> "whois.cnnic.net.cn"
        "tw" -> "whois.twnic.net.tw"
        "hk" -> "whois.hknic.net.hk"
        "sg" -> "whois.nic.net.sg"
        "my" -> "whois.mynic.net.my"
        "th" -> "whois.thnic.net"
        "in" -> "whois.inregistry.net"
        "au" -> "whois.aunic.net"
        "nz" -> "whois.srs.net.nz"
        "za" -> "whois.co.za"
        "br" -> "whois.registro.br"
        "ar" -> "whois.nic.ar"
        "cl" -> "whois.nic.cl"
        "mx" -> "whois.nic.mx"
        "ca" -> "whois.cira.ca"
        else -> "whois.iana.org" // Загальний WHOIS сервер для невідомих доменів
    }
}

private fun isIpAddress(query: String): Boolean {
    val ipPattern = Pattern.compile(
        "^(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    )
    return ipPattern.matcher(query).matches()
}