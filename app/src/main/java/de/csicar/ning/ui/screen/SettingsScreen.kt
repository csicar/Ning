package de.csicar.ning.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import de.csicar.ning.R
import de.csicar.ning.util.AppPreferences

@Composable
fun SettingsScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences(context) }
    var hideMacDetails by remember { mutableStateOf(prefs.hideMacDetails) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.hideMacAddressDetails),
                style = MaterialTheme.typography.bodyLarge
            )
            Switch(
                checked = hideMacDetails,
                onCheckedChange = {
                    hideMacDetails = it
                    prefs.setHideMacDetails(it)
                }
            )
        }
    }
}
