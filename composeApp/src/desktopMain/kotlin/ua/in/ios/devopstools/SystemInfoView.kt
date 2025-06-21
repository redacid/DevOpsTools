
package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SystemInfoView() {
    val systemInfo = SystemInfo.getInstance()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "System Information",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        SystemInfoCard(
            title = "Operating System",
            content = listOf(
                "Name: ${systemInfo.osName}",
                "Version: ${systemInfo.osVersion}",
                "Architecture: ${systemInfo.osArch}",
                "OS Family: ${systemInfo.osFamily}",
                "Distribution: ${systemInfo.distribution}"
            )
        )

        SystemInfoCard(
            title = "Supported Installation Types",
            content = listOf(
                "DEB: ${if (systemInfo.supportsDeb) "Yes" else "No"}",
                "RPM: ${if (systemInfo.supportsRpm) "Yes" else "No"}",
                "DMG: ${if (systemInfo.supportsDmg) "Yes" else "No"}",
                "EXE: ${if (systemInfo.supportsExe) "Yes" else "No"}",
                "MSI: ${if (systemInfo.supportsMsi) "Yes" else "No"}",
                "AppImage: ${if (systemInfo.supportsAppImage) "Yes" else "No"}",
                "Snapcraft: ${if (systemInfo.supportsSnapcraft) "Yes" else "No"}",
                "Flatpak: ${if (systemInfo.supportsFlatpak) "Yes" else "No"}",
                "Default Package Manager Type: ${systemInfo.getDefaultPackageManagerType()}"
            )
        )

        SystemInfoCard(
            title = "Hardware",
            content = listOf(
                "Processor: ${systemInfo.cpuModel}",
                "Core Count: ${systemInfo.availableProcessors}",
                "Total Memory: ${systemInfo.formatMemorySize(systemInfo.totalMemory)}",
                "Free Memory: ${systemInfo.formatMemorySize(systemInfo.freeMemory)}"
            )
        )

        SystemInfoCard(
            title = "User",
            content = listOf(
                "Username: ${systemInfo.userName}",
                "Home Directory: ${systemInfo.userHome}",
                "Package Cache Path: ${systemInfo.packageCachePath}"
            )
        )
    }
}

@Composable
fun SystemInfoCard(title: String, content: List<String>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider(
                modifier = Modifier.padding(bottom = 8.dp),
                thickness = DividerDefaults.Thickness,
                color = DividerDefaults.color
            )

            content.forEach { item ->
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}