package com.sample.android.zoom.ui

import android.annotation.SuppressLint
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import com.sample.android.zoom.utils.AutoFitSurfaceView

@SuppressLint("UnrememberedMutableState")
@Composable
fun CameraPreview(
    modifier: Modifier,
    cameraPreviewViewModel: CameraPreviewViewModel = hiltViewModel(),
) {

    val context = LocalContext.current

    val defaultCameraId: String = "0"

    val surfaceView = AutoFitSurfaceView(context)
    surfaceView.setAspectRatio(1920, 1080)

    var cameraInitialized by remember { mutableStateOf(false) }
    val zoom = cameraPreviewViewModel.zoomFlow.collectAsState()

    LifecycleStartEffect(Unit) {
        cameraPreviewViewModel.init(context, surfaceView, defaultCameraId)
        cameraInitialized = true
        onStopOrDispose { }
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {

        AndroidView(
            modifier = modifier,
            factory = { ctx ->
                FrameLayout(ctx)
            },
            update = {
                it.apply {
                    surfaceView.parent?.let { (it as FrameLayout).removeAllViews() }
                    addView(surfaceView)
                }
            }
        )

        ZoomButtons(
            {
                cameraPreviewViewModel.onZoomClicked(1f)
            },
            {
                cameraPreviewViewModel.onZoomClicked(1.5f)
            },
            {
                cameraPreviewViewModel.onZoomClicked(4f)
            },
        )

        if (cameraInitialized) {
            ZoomSeekBar(
                initialZoom = zoom.value,
                maxZoom = cameraPreviewViewModel.onMaxZoomSupported(),
                onZoomChange = { cameraPreviewViewModel.onZoomChanged(it) }
            )
        }
    }

}

@Composable
fun ZoomButtons(
    onFirstButtonClick: () -> Unit,
    onSecondButtonClick: () -> Unit,
    onThirdButtonClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopStart
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth(0.5f)
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly // Distribute space evenly
        ) {
            Button(
                onClick = onFirstButtonClick,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f) // Makes buttons share horizontal space equally
            ) {
                Text("1x")
            }
            Button(
                onClick = onSecondButtonClick,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                Text("1,5x")
            }
            Button(
                onClick = onThirdButtonClick,
                modifier = Modifier
                    .padding(8.dp)
                    .weight(1f)
            ) {
                Text("4x")
            }
        }
    }
}

@Composable
fun ZoomSeekBar(
    modifier: Modifier = Modifier,
    initialZoom: Float,
    maxZoom: Float = 10f,
    onZoomChange: ((Float) -> Unit)? = null,
) {

    var sliderPosition by remember(initialZoom) { mutableFloatStateOf(initialZoom) }

    Column(
        modifier = modifier
            .fillMaxWidth(0.5f)
            .fillMaxHeight()
            .padding(16.dp),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {

        Text(
            text = "Zoom: %.1f".format(sliderPosition),
            color = MaterialTheme.colorScheme.primary
        )

        Slider(
            value = sliderPosition,
            onValueChange = { newValue ->
                sliderPosition = newValue
                onZoomChange?.invoke(newValue)
            },
            valueRange = 0f..maxZoom,
            modifier = Modifier.fillMaxWidth()
        )
    }
}