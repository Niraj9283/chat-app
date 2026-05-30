package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.SecureMessengerApp
import com.example.ui.SecureMessengerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: SecureMessengerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SecureMessengerApp(viewModel = viewModel)
        }
    }
}
