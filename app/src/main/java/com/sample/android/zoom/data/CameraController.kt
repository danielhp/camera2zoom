/*
 *
 * Created by Daniel Hernández Portugués.
 */

package com.sample.android.zoom.data

import android.annotation.SuppressLint
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraDevice.StateCallback
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.params.OutputConfiguration
import android.hardware.camera2.params.SessionConfiguration
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.android.asCoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.Executor

/**
 * [CameraController] manages the state and operations of a camera device.
 *
 * @property targetCameraId The ID of the target camera.
 * @property cameraManager The [CameraManager] used to interact with the camera system.
 * @property coroutineScope The [CoroutineScope] used for launching coroutines.
 *
 * @constructor Creates a [CameraController] for the specified camera.
 *
 * @throws CameraAccessException If there is an error accessing the camera characteristics.
 */
class CameraController(
    private val coroutineScope: CoroutineScope,
    private val targetCameraId: String,
    private val cameraManager: CameraManager,
    private val characteristics: CameraCharacteristics,
) {

    enum class CameraState { OPENED, CLOSED, SESSION_OPENED, SESSION_READY }

    private val _cameraStateFlow = MutableStateFlow(CameraState.CLOSED)
    val cameraStateFlow: StateFlow<CameraState> = _cameraStateFlow.asStateFlow()

    private class HandlerExecutor(private val handler: Handler) : Executor {
        override fun execute(command: Runnable) {
            handler.post(command)
        }
    }

    private val exceptionHandler = CoroutineExceptionHandler { _, exception ->
        exception.printStackTrace()
        throw exception
    }

    /** [HandlerThread] where all camera operations run */
    private val cameraThread = HandlerThread("\"CamThread-ID${targetCameraId}\"").apply { start() }

    /** [Handler] corresponding to [cameraThread] */
    private val cameraHandler = Handler(cameraThread.looper)

    private val handlerExecutor = HandlerExecutor(cameraHandler)

    private var captureRequest: CaptureRequest? = null

    internal var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    internal var requestBuilder: CaptureRequest.Builder? = null

    internal val captureCallback = object : CameraCaptureSession.CaptureCallback() {

    }

    /**
     * Begin all camera operations in a coroutine in the main thread. This function:
     * - Opens the camera
     * - Configures the camera session
     * - Starts a repeating request
     *
     * @param cameraId of the camera to open
     * @param previewTargets
     * @receiver
     */
    @SuppressLint("MissingPermission")
    fun startCamera(cameraId: String, previewTargets: List<Surface>) {

        coroutineScope.launch(exceptionHandler) {
            withContext(cameraHandler.asCoroutineDispatcher()) {
                if (cameraStateFlow.value != CameraState.CLOSED) throw IllegalStateException("Camera $cameraId already opened.")

                // Open the selected camera
                val cameraStateCallback = object : StateCallback() {
                    override fun onOpened(device: CameraDevice) {
                        _cameraStateFlow.value = CameraState.OPENED
                        cameraDevice = device
                        // immediately create a capture session with the surface targets
                        createSession(device, previewTargets)

                    }

                    override fun onDisconnected(device: CameraDevice) {
                        captureSession?.close()
                        device.close()
                        cameraDevice = null
                        _cameraStateFlow.value = CameraState.CLOSED
                        IllegalStateException("Camera $cameraId disconnected.")
                    }

                    override fun onError(device: CameraDevice, error: Int) {
                        captureSession?.close()
                        device.close()
                        cameraDevice = null
                        _cameraStateFlow.value = CameraState.CLOSED
                        IllegalStateException("Error $error.")
                    }

                    override fun onClosed(device: CameraDevice) {
                        super.onClosed(device)
                        cameraDevice = null
                        _cameraStateFlow.value = CameraState.CLOSED
                    }
                }

                cameraManager.openCamera(cameraId, cameraStateCallback, cameraHandler)
            }
        }
    }

    fun setZoom(zoom: Float) {
        requestBuilder?.apply {
            set(CaptureRequest.CONTROL_ZOOM_RATIO, zoom)
        }
        setRepeatingRequest()
    }


    private fun createSession(cameraDevice: CameraDevice, targets: List<Surface>) {

        val capabilities = characteristics.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)!!

        val stateCallback = object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                _cameraStateFlow.value = CameraState.SESSION_OPENED
                captureSession = session

                // immediately start a capture request on those same targets
                cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
                    // Add all the surface targets. This can be updated during the session, for now we assume all te surfaces are always active.
                    targets.forEach { addTarget(it) }
                    // Capture request holds references to target surfaces.
                    requestBuilder = this
                    captureRequest = build()
                }

                setRepeatingRequest()

                _cameraStateFlow.value = CameraState.SESSION_READY
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                IllegalStateException("Session configuration failed.")
                captureSession = null
            }

            /** Called after all captures have completed */
            override fun onClosed(session: CameraCaptureSession) {
                _cameraStateFlow.value = CameraState.OPENED
                captureSession = null
            }
        }

        val outputList = ArrayList<OutputConfiguration>()
        targets.forEachIndexed { index, surface ->
            outputList.add(
                OutputConfiguration(surface).apply {
                    if (capabilities.contains(CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_STREAM_USE_CASE)) {
                        when (index) {
                            0 -> this.streamUseCase = CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_PREVIEW.toLong()
                            1 -> this.streamUseCase = CameraMetadata.SCALER_AVAILABLE_STREAM_USE_CASES_VIDEO_RECORD.toLong()
                        }
                    }
                }
            )
        }

        val sessionType = SessionConfiguration.SESSION_REGULAR

        val sessionConfig = SessionConfiguration(sessionType, outputList, handlerExecutor, stateCallback)

        return cameraDevice.createCaptureSession(sessionConfig)
    }


    fun setRepeatingRequest(callback: CameraCaptureSession.CaptureCallback = captureCallback) {
        captureSession?.apply {
            requestBuilder?.let {
                if (cameraStateFlow.value == CameraState.SESSION_OPENED || cameraStateFlow.value == CameraState.SESSION_READY) {
                    // Sometimes this method gets called when the session is already closed, it shouldn't happen but it doesn't hurt to catch it.
                    try {
                        stopRepeating()
                        setRepeatingRequest(it.build(), callback, cameraHandler)
                    } catch (_: Exception) {
                    }
                }
            }
        }
    }
}