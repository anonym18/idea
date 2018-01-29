import akka.actor.ActorRef

/**
  * Created by burcu on 4/9/17.
  */
package object server {

  case class RegisterUIServer(ref: ActorRef)

}
