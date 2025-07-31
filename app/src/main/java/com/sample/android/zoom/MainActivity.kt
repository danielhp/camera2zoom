package com.sample.android.zoom

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sample.android.zoom.ui.CameraPreview
import com.sample.android.zoom.ui.theme.Camera2ZoomTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private var permissionGranted by mutableStateOf(false)
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Check if permission is already granted
        permissionGranted = checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

        // If not granted, request permission
        if (!permissionGranted) {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        configureWindow()

        setContent {
            Camera2ZoomTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (permissionGranted) {
                        CameraPreview(modifier = Modifier)
                    } else {
                        // If permission is not granted, display a message and clickable text to request permission
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Camera permission is required to use this feature, click here to grant permission.",
                                modifier = Modifier
                                    .padding(16.dp)
                                    .clickable { cameraPermissionLauncher.launch(Manifest.permission.CAMERA) }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun configureWindow() {
        // Allow to fill the screen
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.BLACK),
            navigationBarStyle = SystemBarStyle.dark(Color.BLACK),
        )

        window.apply {
            // Set a more appropriate animation for rotations
            attributes = attributes.apply {
                rotationAnimation = WindowManager.LayoutParams.ROTATION_ANIMATION_JUMPCUT
            }
            // Keep the screen awake preventing the OS to lock the device automatically.
            addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            insetsController?.apply {
                systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                // Hide the navigation bar for all. In the future it will be discussed if it's hidden only for gesture navigation or for all.
                hide(WindowInsets.Type.navigationBars())
            }
        }
    }
}

