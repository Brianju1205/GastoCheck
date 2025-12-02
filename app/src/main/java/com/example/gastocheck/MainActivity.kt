package com.example.gastocheck

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.gastocheck.ui.theme.screens.home.HomeScreen
import com.example.gastocheck.ui.theme.GastoCheckTheme
import dagger.hilt.android.AndroidEntryPoint
import com.example.gastocheck.ui.theme.navigation.AppNavigation // <--- Importante
import com.example.gastocheck.ui.theme.GastoCheckTheme
@AndroidEntryPoint // <--- ¡ESTA ETIQUETA ES LA MÁS IMPORTANTE PARA HILT!
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge() // Habilita el diseño de borde a borde (barra de estado transparente)
        setContent {
            GastoCheckTheme {
                // Un contenedor superficie que usa el color de fondo del tema
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Aquí llamamos a tu pantalla principal
                   // HomeScreen()
                    AppNavigation()
                }
            }
        }
    }
}