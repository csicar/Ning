package de.csicar.ning.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.csicar.ning.BuildConfig
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import de.csicar.ning.DeviceId
import de.csicar.ning.R
import de.csicar.ning.ScanId
import de.csicar.ning.ScanViewModel
import de.csicar.ning.ui.screen.DeviceDetailScreen
import de.csicar.ning.ui.screen.DeviceListScreen
import de.csicar.ning.ui.screen.ScanDetailScreen
import de.csicar.ning.ui.screen.ScanHistoryScreen
import de.csicar.ning.ui.screen.SettingsScreen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NingApp(viewModel: ScanViewModel = viewModel()) {
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val interfaces = remember { viewModel.fetchAvailableInterfaces() }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val isTopLevel = currentRoute?.startsWith("deviceList") == true ||
                     currentRoute == "scanHistory" ||
                     currentRoute == null

    // Get device for detail screen title
    val deviceIdLong = navBackStackEntry?.arguments?.getLong("deviceId")
    val device by if (deviceIdLong != null && currentRoute?.startsWith("deviceDetail") == true) {
        viewModel.getDevice(DeviceId(deviceIdLong)).collectAsState(initial = null)
    } else {
        remember { mutableStateOf(null) }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = isTopLevel,
        drawerContent = {
            ModalDrawerSheet {
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(R.drawable.ic_network_check_black_48dp),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Version ${BuildConfig.VERSION_NAME}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider()

                // Interfaces section
                Text(
                    text = stringResource(R.string.interfaces_submenu),
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                interfaces.forEach { nic ->
                    NavigationDrawerItem(
                        icon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_settings_ethernet_white_24dp),
                                contentDescription = null
                            )
                        },
                        label = { Text("${nic.interfaceName} - ${nic.address.hostAddress}/${nic.prefix}") },
                        selected = false,
                        onClick = {
                            navController.navigate("deviceList/${nic.interfaceName}") {
                                popUpTo("deviceList/wlan0") { inclusive = true }
                            }
                            scope.launch { drawerState.close() }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                // Scan History
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_network_check_black_48dp),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                    },
                    label = { Text("Scan History") },
                    selected = currentRoute == "scanHistory",
                    onClick = {
                        navController.navigate("scanHistory")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )

                // Settings
                NavigationDrawerItem(
                    icon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings_white_24dp),
                            contentDescription = null
                        )
                    },
                    label = { Text(stringResource(R.string.preferences_submenu)) },
                    selected = currentRoute == "settings",
                    onClick = {
                        navController.navigate("settings")
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        val title = when {
                            currentRoute?.startsWith("deviceDetail") == true -> {
                                val deviceIp = device?.ip?.hostAddress ?: ""
                                stringResource(R.string.title_device_detail, deviceIp)
                            }
                            currentRoute == "scanHistory" -> "Scan History"
                            currentRoute?.startsWith("scanDetail") == true -> "Scan Details"
                            currentRoute == "settings" ->
                                stringResource(R.string.preferences_submenu)
                            else -> {
                                val interfaceName = navBackStackEntry?.arguments?.getString("interfaceName") ?: "<unknown>"
                                stringResource(R.string.title_network_overview, interfaceName)
                            }
                        }
                        Text(title)
                    },
                    navigationIcon = {
                        if (isTopLevel) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu, contentDescription = "Menu")
                            }
                        } else {
                            IconButton(onClick = { navController.navigateUp() }) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                        navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        ) { paddingValues ->
            NavHost(
                navController = navController,
                // TODO: fix this. get default interface name instead of wlan0
                startDestination = "deviceList/wlan0",
                modifier = Modifier.padding(paddingValues)
            ) {
                composable(
                    "deviceList/{interfaceName}",
                    arguments = listOf(navArgument("interfaceName") {
                        type = NavType.StringType
                    })
                ) { backStackEntry ->
                    // TODO: this should not have any default value
                    val interfaceName = backStackEntry.arguments?.getString("interfaceName") ?: "wlan0"
                    DeviceListScreen(
                        viewModel = viewModel,
                        interfaceName = interfaceName,
                        onDeviceClick = { device ->
                            navController.navigate("deviceDetail/${device.deviceId.value}")
                        }
                    )
                }

                composable(
                    "deviceDetail/{deviceId}",
                    arguments = listOf(navArgument("deviceId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val deviceIdLong = backStackEntry.arguments?.getLong("deviceId") ?: return@composable
                    DeviceDetailScreen(
                        viewModel = viewModel,
                        deviceId = DeviceId(deviceIdLong)
                    )
                }

                composable("scanHistory") {
                    ScanHistoryScreen(
                        viewModel = viewModel,
                        onScanClick = { scan ->
                            navController.navigate("scanDetail/${scan.scanId.value}")
                        }
                    )
                }

                composable(
                    "scanDetail/{scanId}",
                    arguments = listOf(navArgument("scanId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val scanIdLong = backStackEntry.arguments?.getLong("scanId") ?: return@composable
                    ScanDetailScreen(
                        viewModel = viewModel,
                        scanId = ScanId(scanIdLong),
                        onDeviceClick = { device ->
                            navController.navigate("deviceDetail/${device.deviceId.value}")
                        }
                    )
                }

                composable("settings") {
                    SettingsScreen()
                }
            }
        }
    }
}
