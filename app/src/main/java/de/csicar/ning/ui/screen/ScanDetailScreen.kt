package de.csicar.ning.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.csicar.ning.DeviceWithName
import de.csicar.ning.Network
import de.csicar.ning.R
import de.csicar.ning.ScanId
import de.csicar.ning.ScanViewModel

@Composable
fun ScanDetailScreen(
    viewModel: ScanViewModel,
    scanId: ScanId,
    onDeviceClick: (DeviceWithName) -> Unit
) {
    val networks by viewModel.getNetworksForScan(scanId).collectAsState(initial = emptyList())

    // For now, just show devices from the first network
    // In a real implementation, you might want to show all networks
    val networkId = networks.firstOrNull()?.networkId
    val devices by if (networkId != null) {
        viewModel.getDevicesForNetwork(networkId).collectAsState(initial = emptyList())
    } else {
        remember { mutableStateOf(emptyList()) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (networks.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        painter = painterResource(R.drawable.ic_network_check_black_48dp),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No networks found in this scan",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Show network info
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    networks.forEach { network ->
                        NetworkInfo(network)
                    }
                }
            }

            // Show devices
            if (devices.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No devices found",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(devices, key = { it.deviceId.value }) { device ->
                        DeviceItem(
                            device = device,
                            onClick = { onDeviceClick(device) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NetworkInfo(network: Network) {
    Column {
        Text(
            text = "${network.interfaceName} - ${network.baseIp.hostAddress}/${network.mask}",
            style = MaterialTheme.typography.titleMedium
        )
        network.ssid?.let {
            Text(
                text = "SSID: $it",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

