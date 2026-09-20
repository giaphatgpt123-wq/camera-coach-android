package vn.cameracoach.app
import android.content.*
import android.provider.MediaStore
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraController(private val context:Context){
 private var selector=CameraSelector.DEFAULT_BACK_CAMERA;private var camera:Camera?=null;private var imageCapture:ImageCapture?=null;private var videoCapture:VideoCapture<Recorder>?=null;private var recording:Recording?=null;private val analysisExecutor:ExecutorService=Executors.newSingleThreadExecutor()
 var isRecording=false;private set
 fun bind(owner:LifecycleOwner,view:PreviewView,analyzer:ImageAnalysis.Analyzer?=null){
  ProcessCameraProvider.getInstance(context).addListener({
   val provider=ProcessCameraProvider.getInstance(context).get();val preview=Preview.Builder().build().also{it.surfaceProvider=view.surfaceProvider}
   imageCapture=ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();videoCapture=VideoCapture.withOutput(Recorder.Builder().build())
   val analysis=analyzer?.let{ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).setTargetResolution(android.util.Size(640,480)).build().also{a->a.setAnalyzer(analysisExecutor,it)}}
   try{provider.unbindAll();camera=if(analysis!=null)provider.bindToLifecycle(owner,selector,preview,imageCapture,videoCapture,analysis) else provider.bindToLifecycle(owner,selector,preview,imageCapture,videoCapture)}catch(_:Exception){}
  },ContextCompat.getMainExecutor(context))
 }
 fun switchCamera(owner:LifecycleOwner,v:PreviewView,a:ImageAnalysis.Analyzer?=null){selector=if(selector==CameraSelector.DEFAULT_BACK_CAMERA)CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA;bind(owner,v,a)}
 fun setZoom(r:Float){val s=camera?.cameraInfo?.zoomState?.value?:return;camera?.cameraControl?.setZoomRatio(r.coerceIn(s.minZoomRatio,s.maxZoomRatio))}
 fun currentZoom()=camera?.cameraInfo?.zoomState?.value?.zoomRatio?:1f
 fun focus(x:Float,y:Float,w:Int,h:Int){if(w<=0||h<=0)return;val p=SurfaceOrientedMeteringPointFactory(w.toFloat(),h.toFloat()).createPoint(x,y);camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(p).setAutoCancelDuration(3,TimeUnit.SECONDS).build())}
 fun toggleTorch():Boolean{val i=camera?.cameraInfo?:return false;if(!i.hasFlashUnit())return false;val n=i.torchState.value!=TorchState.ON;camera?.cameraControl?.enableTorch(n);return n}
 fun takePhoto(ok:(String)->Unit,err:(String)->Unit){val c=imageCapture?:return;val n="CC_"+SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(System.currentTimeMillis());val v=ContentValues().apply{put(MediaStore.Images.Media.DISPLAY_NAME,"$n.jpg");put(MediaStore.Images.Media.MIME_TYPE,"image/jpeg");put(MediaStore.Images.Media.RELATIVE_PATH,"Pictures/CameraCoach")};val o=ImageCapture.OutputFileOptions.Builder(context.contentResolver,MediaStore.Images.Media.EXTERNAL_CONTENT_URI,v).build();c.takePicture(o,ContextCompat.getMainExecutor(context),object:ImageCapture.OnImageSavedCallback{override fun onImageSaved(r:ImageCapture.OutputFileResults)=ok(r.savedUri?.toString()?:n);override fun onError(e:ImageCaptureException)=err(e.message?:"Lỗi chụp")})}
 fun isFrontCamera()=selector==CameraSelector.DEFAULT_FRONT_CAMERA
 fun shutdown(){analysisExecutor.shutdown()}
 fun toggleVideo(audio:Boolean,event:(VideoRecordEvent)->Unit){if(isRecording){recording?.stop();recording=null;isRecording=false;return};val c=videoCapture?:return;val n="CC_"+SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(System.currentTimeMillis());val v=ContentValues().apply{put(MediaStore.Video.Media.DISPLAY_NAME,"$n.mp4");put(MediaStore.Video.Media.MIME_TYPE,"video/mp4");put(MediaStore.Video.Media.RELATIVE_PATH,"Movies/CameraCoach")};val o=MediaStoreOutputOptions.Builder(context.contentResolver,MediaStore.Video.Media.EXTERNAL_CONTENT_URI).setContentValues(v).build();var p:PendingRecording=c.output.prepareRecording(context,o);if(audio)p=p.withAudioEnabled();recording=p.start(ContextCompat.getMainExecutor(context)){event(it)};isRecording=true}
}