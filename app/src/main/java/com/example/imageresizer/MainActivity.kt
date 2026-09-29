package com.example.imageresizer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.imageresizer.ui.ImageResizerScreen
import com.example.imageresizer.ui.ImageResizerViewModel
import com.example.imageresizer.ui.theme.ImageResizerTheme

class MainActivity : ComponentActivity() {

    private val viewModel: ImageResizerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ImageResizerTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ImageResizerScreen(viewModel = viewModel)
                }
            }
        }
    }
}
