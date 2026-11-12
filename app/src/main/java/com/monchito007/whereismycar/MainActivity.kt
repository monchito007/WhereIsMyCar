package com.monchito007.whereismycar

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.monchito007.whereismycar.ui.ParkingScreen
import com.monchito007.whereismycar.ui.theme.WhereIsMyCarTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            WhereIsMyCarTheme {
                ParkingScreen()
            }
        }
    }
}
