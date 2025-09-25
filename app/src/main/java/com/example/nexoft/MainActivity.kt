package com.example.nexoft

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.rememberNavController
import com.example.nexoft.navigation.AppNavHost

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Varsayılan tema dosyan ne ise onu kullan (Material3 template’te hazır gelir)
            Surface(color = MaterialTheme.colorScheme.background) {
                val nav = rememberNavController()
                AppNavHost(navController = nav)
            }
        }
    }
}
