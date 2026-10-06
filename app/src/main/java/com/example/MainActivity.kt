package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.presentation.bootstrap.BootstrapScreen
import com.example.presentation.bootstrap.BootstrapViewModel
import com.example.presentation.shell.AppShellViewModel
import com.example.presentation.shell.ElajxAppShell
import com.example.presentation.theme.ElajxTheme

class MainActivity : ComponentActivity() {
    private val appShellViewModel: AppShellViewModel by viewModels()
    private val bootstrapViewModel: BootstrapViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ElajxTheme {
                ElajxAppShell(
                    viewModel = appShellViewModel,
                    bootstrapViewModel = bootstrapViewModel
                )
            }
        }
    }
}

