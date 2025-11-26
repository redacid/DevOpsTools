package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DismissibleDrawerSheet
import androidx.compose.material3.DismissibleNavigationDrawer
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowState
import com.google.gson.JsonObject
import kotlinx.coroutines.launch


@Composable
fun NavigationDrawer(windowState: WindowState? = null) {
    var currentScreen by remember { mutableStateOf("Home") }

    DetailedDrawer(
        onScreenSelected = { screen ->
            currentScreen = screen
        },
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "Tasks" -> TasksTable()
                "Subnet Calculator" -> SubnetCalculator(modifier = Modifier.fillMaxSize())
                "WHOIS Lookup" -> WhoisLookup(modifier = Modifier.fillMaxSize())
                "JSON Tasks" -> JsonTaskTable()
                "Base64 Tool" -> Base64Tool(modifier = Modifier.fillMaxSize())
                "Color Picker" -> ColorPicker(modifier = Modifier.fillMaxSize(),
                    mainWindowState = windowState)
                "JSON Converter" -> JsonConverter(modifier = Modifier.fillMaxSize())
                "Regex Tester" -> RegexTester(modifier = Modifier.fillMaxSize())
                "Number Base Converter" -> NumberBaseConverter(modifier = Modifier.fillMaxSize())
                "Spiral Antenna Calculator" -> SpiralAntennaCalculator(modifier = Modifier.fillMaxSize())
                "Help" -> Text("Help", modifier = Modifier.padding(16.dp))
                "System Info" -> SystemInfoView()
                "Logs" -> LogViewer(modifier = Modifier.fillMaxSize().padding(16.dp))
                "JSON View" -> JsonViewer()
                else -> TasksTable()
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailedDrawer(
    onScreenSelected: (String) -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var tasks by remember { mutableStateOf(emptyList<JsonObject>()) }
    var selectedItem by remember { mutableStateOf("Home") }

    DismissibleNavigationDrawer(
        drawerContent = {
            DismissibleDrawerSheet{
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))

                    NavigationDrawerItem(
                        label = { Text("Tasks") },
                        selected = selectedItem == "Tasks",
                        icon = { Icon(ICON_CONNECT, contentDescription = null) },
                        onClick = {
                            selectedItem = "Tasks"
                            onScreenSelected("Tasks")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("System information") },
                        selected = selectedItem == "System Info",
                        icon = { Icon(ICON_INFO, contentDescription = null) },
                        onClick = {
                            selectedItem = "System Info"
                            onScreenSelected("System Info")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Logs") },
                        selected = selectedItem == "Logs",
                        icon = { Icon(ICON_LOGS, contentDescription = null) },
                        onClick = {
                            selectedItem = "Logs"
                            onScreenSelected("Logs")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    NavigationDrawerItem(
                        label = { Text("Subnet Calculator") },
                        selected = selectedItem == "Subnet Calculator",
                        icon = { Icon(ICON_HASH, contentDescription = null) },
                        onClick = {
                            selectedItem = "Subnet Calculator"
                            onScreenSelected("Subnet Calculator")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("WHOIS Lookup") },
                        selected = selectedItem == "WHOIS Lookup",
                        icon = { Icon(ICON_SEARCH, contentDescription = null) },
                        onClick = {
                            selectedItem = "WHOIS Lookup"
                            onScreenSelected("WHOIS Lookup")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Base64 Tool") },
                        selected = selectedItem == "Base64 Tool",
                        icon = { Icon(ICON_CODE, contentDescription = null) }, // або ICON_TRANSFORM
                        onClick = {
                            selectedItem = "Base64 Tool"
                            onScreenSelected("Base64 Tool")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Color Picker") },
                        selected = selectedItem == "Color Picker",
                        icon = { Icon(ICON_FEATHER, contentDescription = null) }, // або використайте інші іконки якщо немає цієї
                        onClick = {
                            selectedItem = "Color Picker"
                            onScreenSelected("Color Picker")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("JSON Converter") },
                        selected = selectedItem == "JSON Converter",
                        icon = { Icon(ICON_CODE, contentDescription = null) },
                        onClick = {
                            selectedItem = "JSON Converter"
                            onScreenSelected("JSON Converter")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Regex Tester") },
                        selected = selectedItem == "Regex Tester",
                        icon = { Icon(ICON_SEARCH, contentDescription = null) },
                        onClick = {
                            selectedItem = "Regex Tester"
                            onScreenSelected("Regex Tester")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Number Base Converter") },
                        selected = selectedItem == "Number Base Converter",
                        icon = { Icon(ICON_HASH, contentDescription = null) },
                        onClick = {
                            selectedItem = "Number Base Converter"
                            onScreenSelected("Number Base Converter")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Spiral Antenna Calculator") },
                        selected = selectedItem == "Spiral Antenna Calculator",
                        icon = { Icon(ICON_CONNECT, contentDescription = null) },
                        onClick = {
                            selectedItem = "Spiral Antenna Calculator"
                            onScreenSelected("Spiral Antenna Calculator")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text("Help") },
                        selected = selectedItem == "Help",
                        icon = { Icon(ICON_HELP, contentDescription = null) },
                        onClick = {
                            selectedItem = "Help"
                            onScreenSelected("Help")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    if (settingsManager.getString("settings.log_level") == "DEV") {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                        NavigationDrawerItem(
                            label = { Text("DEV JSON View") },
                            selected = selectedItem == "JSON View",
                            icon = { Icon(ICON_CODE, contentDescription = null) },
                            onClick = {
                                selectedItem = "JSON View"
                                onScreenSelected("JSON View")
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        )

                        NavigationDrawerItem(
                            label = { Text("DEV JSON Tasks Table") },
                            selected = selectedItem == "JSON Tasks",
                            icon = { Icon(ICON_BOX, contentDescription = null) },
                            onClick = {
                                selectedItem = "JSON Tasks"
                                onScreenSelected("JSON Tasks")
                                scope.launch {
                                    drawerState.close()
                                }
                            }
                        )
                    }

                    Spacer(Modifier.height(12.dp))
                }
            }
        },
        drawerState = drawerState
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("DevOps Tools") },
                    navigationIcon = {
                        IconButton(onClick = {
                            scope.launch {
                                if (drawerState.isClosed) {
                                    drawerState.open()
                                } else {
                                    drawerState.close()
                                }
                            }
                        }) {
                            Icon(ICON_MENU, contentDescription = "Menu")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}

