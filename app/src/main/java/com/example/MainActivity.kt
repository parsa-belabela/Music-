package com.example

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.example.ui.screens.MainScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.MusicPlayerViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MusicPlayerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val appSettings by viewModel.appSettings.collectAsState()

            // Request necessary permissions at startup
            val permissionLauncher = rememberLauncherForActivityResult(
                contract = ActivityResultContracts.RequestMultiplePermissions()
            ) { permissions ->
                val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
                } else {
                    permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
                }
                if (audioGranted) {
                    viewModel.scanDeviceLibrary()
                }
            }

            LaunchedEffect(Unit) {
                val permissionsToRequest = mutableListOf<String>()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                    permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
                } else {
                    permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
                permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)

                permissionLauncher.launch(permissionsToRequest.toTypedArray())
            }

            MyApplicationTheme(
                appTheme = appSettings.theme,
                accentColor = Color(appSettings.customAccentColor)
            ) {
                MainScreen(
                    viewModel = viewModel,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
