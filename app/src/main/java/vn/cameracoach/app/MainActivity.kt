package vn.cameracoach.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme { CameraApp() } }
    }
}

@Composable
fun CameraApp() {
    val context = LocalContext.current
    val owner = LocalLifecycleOwner.current
    var cameraGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var audioGranted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        cameraGranted = result[Manifest.permission.CAMERA] == true || cameraGranted
        audioGranted = result[Manifest.permission.RECORD_AUDIO] == true || audioGranted
    }
    LaunchedEffect(Unit) {
        if (!cameraGranted) permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO))
    }
    if (!cameraGranted) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Camera Coach cần quyền Camera để hoạt động.")
                Spacer(Modifier.height(12.dp))
                Button(onClick = { permissionLauncher.launch(arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)) }) {
                    Text("Cấp quyền")
                }
            }
        }
        return
    }

    val controller = remember { CameraController(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var videoMode by remember { mutableStateOf(false) }
    var recording by remember { mutableStateOf(false) }
    var torch by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                    previewView = this
                    controller.bind(owner, this)
                }
            },
            modifier = Modifier.fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { p -> previewView?.let { controller.focus(p.x, p.y, it.width, it.height) } }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, _, zoom, _ ->
                        if (zoom != 1f) controller.setZoom(controller.currentZoom() * zoom)
                    }
                }
        )

        Row(
            Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            AssistChip(onClick = {}, label = { Text("AUTO") })
            AssistChip(onClick = { torch = controller.toggleTorch() }, label = { Text(if (torch) "Flash ON" else "Flash") })
        }

        Column(
            Modifier.fillMaxWidth().padding(20.dp).align(Alignment.BottomCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { videoMode = false }) { Text("ẢNH", color = if (!videoMode) Color.Yellow else Color.White) }
                Button(onClick = {
                    if (videoMode) {
                        if (!audioGranted && !recording) {
                            permissionLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                        } else {
                            controller.toggleVideo(audioGranted) { event ->
                                if (event is androidx.camera.video.VideoRecordEvent.Finalize) {
                                    Toast.makeText(context, "Đã lưu video", Toast.LENGTH_SHORT).show()
                                }
                            }
                            recording = controller.isRecording
                        }
                    } else {
                        controller.takePhoto(
                            { Toast.makeText(context, "Đã lưu ảnh", Toast.LENGTH_SHORT).show() },
                            { Toast.makeText(context, it, Toast.LENGTH_SHORT).show() }
                        )
                    }
                }) { Text(if (videoMode && recording) "DỪNG" else if (videoMode) "QUAY" else "CHỤP") }
                TextButton(onClick = { videoMode = true }) { Text("VIDEO", color = if (videoMode) Color.Yellow else Color.White) }
            }
            Spacer(Modifier.height(10.dp))
            TextButton(onClick = { previewView?.let { controller.switchCamera(owner, it) } }) {
                Text("Đổi camera", color = Color.White)
            }
        }
    }
}
