package vn.cameracoach.app
import android.annotation.SuppressLint
import androidx.camera.core.ImageAnalysis
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

data class PortraitState(val hasFace:Boolean=false,val cx:Float=.5f,val cy:Float=.5f,val area:Float=0f,val score:Int=0,val guidance:String="TÌM KHUÔN MẶT")

class PortraitCoachEngine(private val onUpdate:(PortraitState)->Unit){
 private val busy=AtomicBoolean(false)
 private val detector=FaceDetection.getClient(FaceDetectorOptions.Builder().setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST).setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE).setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE).enableTracking().build())
 @SuppressLint("UnsafeOptInUsageError")
 val analyzer=ImageAnalysis.Analyzer{proxy->
  val media=proxy.image
  if(media==null||!busy.compareAndSet(false,true)){proxy.close();return@Analyzer}
  val image=InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)
  detector.process(image).addOnSuccessListener{faces->
   val face=faces.maxByOrNull{it.boundingBox.width()*it.boundingBox.height()}
   onUpdate(if(face==null)PortraitState() else score(face,image.width.toFloat(),image.height.toFloat()))
  }.addOnCompleteListener{busy.set(false);proxy.close()}
 }
 private fun score(face:Face,w:Float,h:Float):PortraitState{
  val b=face.boundingBox;val cx=(b.centerX()/w).coerceIn(0f,1f);val cy=(b.centerY()/h).coerceIn(0f,1f);val area=((b.width()*b.height())/(w*h)).coerceIn(0f,1f)
  val centerScore=(100-abs(cx-.5f)*170).toInt().coerceIn(0,100)
  val eyeLineScore=(100-abs(cy-.38f)*180).toInt().coerceIn(0,100)
  val sizeScore=(100-abs(area-.16f)*220).toInt().coerceIn(0,100)
  val score=(centerScore*.35f+eyeLineScore*.4f+sizeScore*.25f).toInt()
  val guide=when{area<.045f->"TIẾN GẦN KHUÔN MẶT";area>.42f->"LÙI LẠI";cy<.24f->"HẠ MÁY";cy>.58f->"NÂNG MÁY";cx<.34f->"DỊCH TRÁI";cx>.66f->"DỊCH PHẢI";score>=84->"CHÂN DUNG TỐT ✓";else->"TINH CHỈNH CHÂN DUNG"}
  return PortraitState(true,cx,cy,area,score,guide)
 }
 fun close(){detector.close()}
}