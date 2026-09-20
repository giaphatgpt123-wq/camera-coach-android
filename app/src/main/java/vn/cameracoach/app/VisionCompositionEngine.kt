package vn.cameracoach.app
import android.annotation.SuppressLint
import androidx.camera.core.*
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs

data class CompositionState(val hasSubject:Boolean=false,val cx:Float=.5f,val cy:Float=.5f,val area:Float=0f,val score:Int=0,val guidance:String="TÌM CHỦ THỂ")

class VisionCompositionEngine(private val onUpdate:(CompositionState)->Unit){
 private val busy=AtomicBoolean(false); private val executor=Executors.newSingleThreadExecutor()
 private val detector=ObjectDetection.getClient(ObjectDetectorOptions.Builder().setDetectorMode(ObjectDetectorOptions.STREAM_MODE).enableMultipleObjects().build())
 @SuppressLint("UnsafeOptInUsageError")
 val analyzer=ImageAnalysis.Analyzer { proxy ->
  val media=proxy.image
  if(media==null||!busy.compareAndSet(false,true)){proxy.close();return@Analyzer}
  val image=InputImage.fromMediaImage(media,proxy.imageInfo.rotationDegrees)
  detector.process(image).addOnSuccessListener{objects->
   val w=image.width.toFloat();val h=image.height.toFloat()
   val best=objects.maxByOrNull{it.boundingBox.width()*it.boundingBox.height()}
   if(best==null)onUpdate(CompositionState())
   else{
    val b=best.boundingBox;val cx=b.centerX()/w;val cy=b.centerY()/h;val area=(b.width()*b.height())/(w*h)
    val thirds=listOf(1f/3f,2f/3f);val dx=thirds.minOf{abs(cx-it)};val dy=thirds.minOf{abs(cy-it)}
    val pos=(100-(dx+dy)*180).toInt().coerceIn(0,100);val size=(100-abs(area-.22f)*180).toInt().coerceIn(0,100);val score=(pos*.7f+size*.3f).toInt()
    val guide=when{area<.07f->"TIẾN GẦN";area>.55f->"LÙI LẠI";cx<.28f->"DỊCH MÁY TRÁI";cx>.72f->"DỊCH MÁY PHẢI";cy<.22f->"HẠ MÁY";cy>.78f->"NÂNG MÁY";score>=82->"GÓC TỐT ✓";else->"TINH CHỈNH BỐ CỤC"}
    onUpdate(CompositionState(true,cx,cy,area,score,guide))
   }
  }.addOnCompleteListener{busy.set(false);proxy.close()}
 }
 fun close(){detector.close();executor.shutdown()}
}