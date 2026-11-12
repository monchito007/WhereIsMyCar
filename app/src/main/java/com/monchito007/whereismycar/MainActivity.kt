package com.monchito007.whereismycar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.monchito007.whereismycar.ui.AutoParkSettingsScreen
import com.monchito007.whereismycar.ui.ParkingScreen
import com.monchito007.whereismycar.ui.theme.WhereIsMyCarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhereIsMyCarTheme {
                var showAutoParkSettings by rememberSaveable { mutableStateOf(false) }
                if (showAutoParkSettings) {
                    AutoParkSettingsScreen(onBack = { showAutoParkSettings = false })
                } else {
                    ParkingScreen(onOpenAutoParkSettings = { showAutoParkSettings = true })
                }
            }
        }
    }
}
