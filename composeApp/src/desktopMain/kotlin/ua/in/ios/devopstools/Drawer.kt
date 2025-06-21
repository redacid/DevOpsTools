package ua.`in`.ios.devopstools

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
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
import com.google.gson.JsonObject
import kotlinx.coroutines.launch


@Composable
fun NavigationDrawer() {
    var currentScreen by remember { mutableStateOf("Home") }

    DetailedDrawer(
        onScreenSelected = { screen ->
            currentScreen = screen
        }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
            when (currentScreen) {
                "Tasks" -> TasksTable()
                "Settings" -> Text(
                    "Налаштування",
                    modifier = Modifier.padding(16.dp)
                )
                "Help" -> Text(
                    "Допомога та зворотній зв'язок",
                    modifier = Modifier.padding(16.dp)
                )
                "Update Tasks" -> Text(
                    "Updated Tasks",
                    modifier = Modifier.padding(16.dp)
                )
                "System Info" -> SystemInfoView()
                else -> TasksTable()
//                    Text(
//                    "DevOps Tools - допомога у встановленні та оновленні DevOps інструментів",
//                    modifier = Modifier.padding(16.dp)
//                )
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
    val tasksManager = TasksManager.getInstance()
    var tasks by remember { mutableStateOf(emptyList<JsonObject>()) }
    var selectedItem by remember { mutableStateOf("Home") }

    ModalNavigationDrawer(
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Spacer(Modifier.height(12.dp))

                    NavigationDrawerItem(
                        label = { Text("Завдання") },
                        selected = selectedItem == "Tasks",
                        onClick = {
                            selectedItem = "Tasks"
                            onScreenSelected("Tasks")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Перевірити оновлення") },
                        selected = selectedItem == "Updates",
                        onClick = {
                            selectedItem = "Updates"
                            onScreenSelected("Updates")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )
                    NavigationDrawerItem(
                        label = { Text("Системна інформація") },
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

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    NavigationDrawerItem(
                        label = { Text("Налаштування") },
                        selected = selectedItem == "Settings",
                        icon = { Icon(ICON_SETTINGS, contentDescription = null) },
                        onClick = {
                            selectedItem = "Settings"
                            onScreenSelected("Settings")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    NavigationDrawerItem(
                        label = {Text("Оновити завдання")},
                        selected = selectedItem == "Update Tasks",
                        icon = { Icon(ICON_REFRESH, contentDescription = null) },
                        onClick = {
                            tasksManager.reloadTasks()
                            selectedItem = "Update Tasks"
                            onScreenSelected("Update Tasks")
                            scope.launch {
                                drawerState.close()
                            }
                        }
                    )

                    NavigationDrawerItem(
                        label = { Text("Допомога") },
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
                            Icon(ICON_MENU, contentDescription = "Меню")
                        }
                    }
                )
            }
        ) { innerPadding ->
            content(innerPadding)
        }
    }
}

