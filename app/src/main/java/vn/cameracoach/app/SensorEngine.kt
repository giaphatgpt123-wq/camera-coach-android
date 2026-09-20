package vn.cameracoach.app
import android.content.Context
import android.hardware.*
import kotlin.math.*

data class LevelState(val rollDegrees:Float=0f,val motion:Float=0f,val stable:Boolean=false,val guidance:String="Đang đo...")

class SensorEngine(context:Context, private val onUpdate:(LevelState)->Unit):SensorEventListener{
 private val manager=context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
 private val accelerometer=manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
 private var gravity=floatArrayOf(0f,0f,0f); private var last=floatArrayOf(0f,0f,0f); private var initialized=false
 fun start(){ accelerometer?.let{manager.registerListener(this,it,SensorManager.SENSOR_DELAY_GAME)}}
 fun stop()=manager.unregisterListener(this)
 override fun onAccuracyChanged(sensor:Sensor?,accuracy:Int)=Unit
 override fun onSensorChanged(event:SensorEvent){
  if(event.sensor.type!=Sensor.TYPE_ACCELEROMETER)return
  val alpha=.85f
  for(i in 0..2)gravity[i]=alpha*gravity[i]+(1-alpha)*event.values[i]
  var roll=Math.toDegrees(atan2(gravity[0].toDouble(),gravity[1].toDouble())).toFloat()
  if(roll>90)roll-=180 else if(roll< -90)roll+=180
  var motion=0f
  if(initialized){val dx=event.values[0]-last[0];val dy=event.values[1]-last[1];val dz=event.values[2]-last[2];motion=sqrt(dx*dx+dy*dy+dz*dz)}
  last=event.values.clone();initialized=true
  val level=abs(roll)<=2f;val still=motion<.35f
  val guide=when{!still->"GIỮ YÊN";roll>2->"XOAY TRÁI "+abs(roll).toInt()+"°";roll< -2->"XOAY PHẢI "+abs(roll).toInt()+"°";else->"CÂN BẰNG ✓"}
  onUpdate(LevelState(roll,motion,level&&still,guide))
 }
}