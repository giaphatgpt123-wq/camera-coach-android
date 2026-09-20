package vn.cameracoach.app

import android.content.ContentValues
import android.content.Context
import android.provider.MediaStore
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.MediaStoreOutputOptions
import androidx.camera.video.PendingRecording
import androidx.camera.video.Recorder
import androidx.camera.video.Recording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

class CameraController(private val context: Context) {
    private var selector = CameraSelector.DEFAULT_BACK_CAMERA
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    var isRecording = false
        private set

    fun bind(owner: LifecycleOwner, previewView: PreviewView, onReady: () -> Unit = {}) {
        val providerFuture = ProcessCameraProvider.getInstance(context)
        providerFuture.addListener({
            val provider = providerFuture.get()
            val preview = Preview.Builder().build().also {
                it.surfaceProvider = previewView.surfaceProvider
            }
            imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val recorder = Recorder.Builder().build()
            videoCapture = VideoCapture.withOutput(recorder)
            try {
                provider.unbindAll()
                camera = provider.bindToLifecycle(owner, selector, preview, imageCapture, videoCapture)
                onReady()
            } catch (_: Exception) { }
        }, ContextCompat.getMainExecutor(context))
    }

    fun switchCamera(owner: LifecycleOwner, previewView: PreviewView) {
        selector = if (selector == CameraSelector.DEFAULT_BACK_CAMERA)
            CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        bind(owner, previewView)
    }

    fun setZoom(ratio: Float) {
        val state = camera?.cameraInfo?.zoomState?.value ?: return
        camera?.cameraControl?.setZoomRatio(ratio.coerceIn(state.minZoomRatio, state.maxZoomRatio))
    }

    fun currentZoom(): Float = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f

    fun focus(x: Float, y: Float, width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        val factory = SurfaceOrientedMeteringPointFactory(width.toFloat(), height.toFloat())
        val point = factory.createPoint(x, y)
        val action = FocusMeteringAction.Builder(point)
            .setAutoCancelDuration(3, TimeUnit.SECONDS).build()
        camera?.cameraControl?.startFocusAndMetering(action)
    }

    fun toggleTorch(): Boolean {
        val info = camera?.cameraInfo ?: return false
        if (!info.hasFlashUnit()) return false
        val next = info.torchState.value != androidx.camera.core.TorchState.ON
        camera?.cameraControl?.enableTorch(next)
        return next
    }

    fun takePhoto(onSaved: (String) -> Unit, onError: (String) -> Unit) {
        val capture = imageCapture ?: return
        val name = "CC_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$name.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraCoach")
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            context.contentResolver, MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values
        ).build()
        capture.takePicture(output, ContextCompat.getMainExecutor(context),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(result: ImageCapture.OutputFileResults) =
                    onSaved(result.savedUri?.toString() ?: name)
                override fun onError(exception: ImageCaptureException) =
                    onError(exception.message ?: "Không thể chụp ảnh")
            })
    }

    fun toggleVideo(withAudio: Boolean, onEvent: (VideoRecordEvent) -> Unit) {
        if (isRecording) {
            recording?.stop()
            recording = null
            isRecording = false
            return
        }
        val capture = videoCapture ?: return
        val name = "CC_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(System.currentTimeMillis())
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, "$name.mp4")
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraCoach")
        }
        val options = MediaStoreOutputOptions.Builder(
            context.contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        ).setContentValues(values).build()
        var pending: PendingRecording = capture.output.prepareRecording(context, options)
        if (withAudio) pending = pending.withAudioEnabled()
        recording = pending.start(ContextCompat.getMainExecutor(context)) { onEvent(it) }
        isRecording = true
    }
}
