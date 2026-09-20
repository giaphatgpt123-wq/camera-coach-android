package vn.cameracoach.app
data class AutoCaptureState(val ready:Boolean=false,val progress:Float=0f,val label:String="AUTO")
class AutoCaptureEngine(private val holdMs:Long=800L,private val cooldownMs:Long=3000L){
 private var since=0L;private var last=0L
 fun update(score:Int,stable:Boolean,hasSubject:Boolean,now:Long=System.currentTimeMillis()):Pair<AutoCaptureState,Boolean>{
  val good=hasSubject&&stable&&score>=82
  if(!good){since=0L;return AutoCaptureState(false,0f,"AUTO") to false}
  if(since==0L)since=now
  val progress=((now-since).toFloat()/holdMs).coerceIn(0f,1f)
  val fire=progress>=1f&&now-last>=cooldownMs
  if(fire){last=now;since=0L}
  val label=if(fire)"CHỤP ✓" else "GIỮ YÊN "+(progress*100).toInt()+"%"
  return AutoCaptureState(true,progress,label) to fire
 }
 fun reset(){since=0L}
}