package de.csicar.ning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import de.csicar.ning.ui.NingApp
import de.csicar.ning.ui.theme.NingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NingTheme {
                NingApp()
            }
        }
    }
}
