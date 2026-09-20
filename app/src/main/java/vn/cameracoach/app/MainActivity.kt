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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

class MainActivity:ComponentActivity(){override fun onCreate(savedInstanceState:Bundle?){super.onCreate(savedInstanceState);setContent{MaterialTheme{CameraApp()}}}}

@Composable fun CameraApp(){
 val context=LocalContext.current;val owner=LocalLifecycleOwner.current
 var cameraGranted by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.CAMERA)==PackageManager.PERMISSION_GRANTED)}
 var audioGranted by remember{mutableStateOf(ContextCompat.checkSelfPermission(context,Manifest.permission.RECORD_AUDIO)==PackageManager.PERMISSION_GRANTED)}
 val permissions=rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()){r->cameraGranted=r[Manifest.permission.CAMERA]==true||cameraGranted;audioGranted=r[Manifest.permission.RECORD_AUDIO]==true||audioGranted}
 LaunchedEffect(Unit){if(!cameraGranted)permissions.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO))}
 if(!cameraGranted){Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){Button(onClick={permissions.launch(arrayOf(Manifest.permission.CAMERA,Manifest.permission.RECORD_AUDIO))}){Text("Cấp quyền Camera")}};return}
 val controller=remember{CameraController(context)};var pv by remember{mutableStateOf<PreviewView?>(null)}
 var video by remember{mutableStateOf(false)};var recording by remember{mutableStateOf(false)};var torch by remember{mutableStateOf(false)}
 var level by remember{mutableStateOf(LevelState())};val sensors=remember{SensorEngine(context){level=it}}\n var composition by remember{mutableStateOf(CompositionState())};val vision=remember{VisionCompositionEngine{composition=it}}
 DisposableEffect(Unit){sensors.start();onDispose{sensors.stop();vision.close()}}
 Box(Modifier.fillMaxSize().background(Color.Black)){
  AndroidView(factory={ctx->PreviewView(ctx).apply{scaleType=PreviewView.ScaleType.FILL_CENTER;pv=this;controller.bind(owner,this,vision.analyzer)}},modifier=Modifier.fillMaxSize().pointerInput(Unit){detectTapGestures{p->pv?.let{controller.focus(p.x,p.y,it.width,it.height)}}}.pointerInput(Unit){detectTransformGestures{_,_,z,_->if(z!=1f)controller.setZoom(controller.currentZoom()*z)}})
  Canvas(Modifier.fillMaxSize()){val c=Offset(size.width/2,size.height*.48f);val h=size.width*.28f;val dy=(level.rollDegrees.coerceIn(-15f,15f)/15f)*45f;drawLine(if(level.stable)Color.Green else Color.White,Offset(c.x-h,c.y-dy),Offset(c.x+h,c.y+dy),5f);drawCircle(Color.White,5f,c)}
  Canvas(Modifier.fillMaxSize()){val w=size.width;val h=size.height;val c=Color.White.copy(alpha=.45f);drawLine(c,Offset(w/3,0f),Offset(w/3,h),2f);drawLine(c,Offset(2*w/3,0f),Offset(2*w/3,h),2f);drawLine(c,Offset(0f,h/3),Offset(w,h/3),2f);drawLine(c,Offset(0f,2*h/3),Offset(w,2*h/3),2f);if(composition.hasSubject)drawCircle(if(composition.score>=82)Color.Green else Color.Yellow,22f,Offset(composition.cx*w,composition.cy*h),4f)}
  Column(Modifier.align(Alignment.TopCenter).padding(top=68.dp),horizontalAlignment=Alignment.CenterHorizontally){Surface(color=Color.Black.copy(alpha=.65f)){Text(composition.guidance+"  "+composition.score+"/100",color=if(composition.score>=82)Color.Green else Color.White,modifier=Modifier.padding(horizontal=18.dp,vertical=8.dp))};Spacer(Modifier.height(6.dp));Surface(color=Color.Black.copy(alpha=.5f)){Text(level.guidance,color=if(level.stable)Color.Green else Color.White,modifier=Modifier.padding(horizontal=14.dp,vertical=6.dp))}}
  Row(Modifier.fillMaxWidth().padding(16.dp).align(Alignment.TopCenter),horizontalArrangement=Arrangement.SpaceBetween){AssistChip(onClick={},label={Text("COACH")});AssistChip(onClick={torch=controller.toggleTorch()},label={Text(if(torch)"Flash ON" else "Flash")})}
  Column(Modifier.fillMaxWidth().padding(20.dp).align(Alignment.BottomCenter),horizontalAlignment=Alignment.CenterHorizontally){
   Row(horizontalArrangement=Arrangement.spacedBy(20.dp),verticalAlignment=Alignment.CenterVertically){
    TextButton(onClick={video=false}){Text("ẢNH",color=if(!video)Color.Yellow else Color.White)}
    Button(onClick={if(video){if(!audioGranted&&!recording)permissions.launch(arrayOf(Manifest.permission.RECORD_AUDIO)) else{controller.toggleVideo(audioGranted){e->if(e is androidx.camera.video.VideoRecordEvent.Finalize)Toast.makeText(context,"Đã lưu video",Toast.LENGTH_SHORT).show()};recording=controller.isRecording}}else controller.takePhoto({Toast.makeText(context,"Đã lưu ảnh",Toast.LENGTH_SHORT).show()},{Toast.makeText(context,it,Toast.LENGTH_SHORT).show()})}){Text(if(video&&recording)"DỪNG" else if(video)"QUAY" else "CHỤP")}
    TextButton(onClick={video=true}){Text("VIDEO",color=if(video)Color.Yellow else Color.White)}
   }
   TextButton(onClick={pv?.let{controller.switchCamera(owner,it,vision.analyzer)}}){Text("Đổi camera",color=Color.White)}
  }
 }
}