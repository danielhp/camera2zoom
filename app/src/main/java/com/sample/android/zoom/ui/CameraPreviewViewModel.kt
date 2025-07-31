package com.sample.android.zoom.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import com.sample.android.zoom.domain.CameraSession
import com.sample.android.zoom.utils.AutoFitSurfaceView
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class CameraPreviewViewModel @Inject constructor(private val scope: CoroutineScope) : ViewModel() {

    private lateinit var cameraSession: CameraSession

    private val _zoomFlow = MutableStateFlow<Float>(0f)
    val zoomFlow: StateFlow<Float> = _zoomFlow.asStateFlow()

    fun init(context: Context, surfaceView: AutoFitSurfaceView, cameraId: String) {

        cameraSession =
            CameraSession(context = context, coroutineScope = scope, surfaceView = surfaceView, cameraId = cameraId)
    }

    fun onZoomClicked(zoom: Float) {
        _zoomFlow.update { zoom }
        cameraSession.setZoom(zoom)
    }

    fun onZoomChanged(zoom: Float) {
        _zoomFlow.update { zoom }
        cameraSession.setZoom(zoom)
    }

    fun onMaxZoomSupported(): Float {
        return cameraSession.getMaxZoom()
    }

}