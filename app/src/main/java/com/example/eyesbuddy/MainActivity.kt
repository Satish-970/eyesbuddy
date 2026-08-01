package com.example.eyesbuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.eyesbuddy.ui.EyesScreen

/**
 * Single-activity app. Everything else (sensors, animation, emotion logic)
 * lives inside EyesScreen and the classes it wires together - this class is
 * intentionally thin.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                color = Color.Black
            ) {
                EyesScreen()
            }
        }
    }
}
