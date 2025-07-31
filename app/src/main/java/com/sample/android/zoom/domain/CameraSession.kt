package com.sample.android.zoom.domain

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.MediaRecorder
import android.view.Surface
import android.view.SurfaceHolder
import com.sample.android.zoom.data.CameraController
import com.sample.android.zoom.utils.AutoFitSurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException


class CameraSession(
    private val context: Context,
    private val coroutineScope: CoroutineScope,
    private val surfaceView: AutoFitSurfaceView,
    private val cameraId: String
) {

    private var previewSurface: Surface? = surfaceView.holder.surface
    private var mediaRecorder: MediaRecorder? = null

    private val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    private val characteristics = cameraManager.getCameraCharacteristics(cameraId)

    private val cameraController = CameraController(
        coroutineScope = coroutineScope,
        targetCameraId = cameraId,
        cameraManager = cameraManager,
        characteristics = characteristics
    )

    private val surfaceListener = object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
            // To ensure that size is set, initialize camera in the view's thread
            coroutineScope.launch { initializeCamera() }
        }

        override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            surfaceView.setAspectRatio(width, height)

        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {}
    }

    init {
        surfaceView.apply {
            holder.setFixedSize(1920, 1080)
            holder.addCallback(surfaceListener)
        }
    }

    fun setZoom(zoom: Float) {
        cameraController.setZoom(zoom)
    }

    fun getMaxZoom(): Float {
        return characteristics.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) ?: 1.0f
    }

    private fun initializeCamera() = coroutineScope.launch(Dispatchers.Main) {

        // Use MediaRecorder for recorder surface
        mediaRecorder = MediaRecorder().apply {
           // setAudioSource(MediaRecorder.AudioSource.MIC)
            setVideoSource(MediaRecorder.VideoSource.SURFACE)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)

            setOutputFile(File(context.getExternalFilesDir(null), "VID_${System.currentTimeMillis()}.mp4").absolutePath)

            setVideoEncodingBitRate(10_000_000) // 10 Mbps
            setVideoFrameRate(30)
            setVideoSize(1920, 1080)
           // setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)

            // Set orientation hint for video rotation
            setOrientationHint(getOrientationHint())

            try {
                prepare()
            } catch (e: IOException) {
                e.printStackTrace()
                throw e
            }
        }

        val targets = listOf(previewSurface!!, mediaRecorder?.surface!!)

        cameraController.startCamera(cameraId, targets)

        cameraController.setRepeatingRequest()
    }

    private fun getOrientationHint(): Int {
        val characteristics = cameraManager.getCameraCharacteristics(cameraId!!)
        val sensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 0
        return sensorOrientation
    }
}