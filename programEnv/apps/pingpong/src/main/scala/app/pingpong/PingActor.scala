package app.pingpong

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import akka.dispatch.UnhandledExceptionLogging
import debugger.protocol.{State, StateColor}

class PingActor(pongActor: ActorRef, maxCount: Int, willTerminate: Boolean) extends Actor
  with ActorLogging with UnhandledExceptionLogging {
  import PingActor._
  
  var counter = 0

  def receive: PartialFunction[Any, Unit] = {
  	case Initialize => 
	    log.info("In PingActor - starting ping-pong " + Thread.currentThread().getName)
  	  pongActor ! PingMessage("ping 0")
      self ! Dummy1
  	case PongActor.PongMessage(text) =>
  	  log.info("In PingActor - received message: " + text + " in " + Thread.currentThread().getName)
  	  counter += 1
      if (counter <= maxCount) {
        sender() ! PingMessage("ping " + counter)
      }
      else if (willTerminate) context.stop(self)

    case GetInternalActorState => sender() ! getState

    case Dummy1 => println("Got dummy message 1")
      self ! Dummy2

    case Dummy2 => println("Got dummy message 2")
  }

  def getState : State = State(self.path.name,
    if (counter < maxCount) StateColor(0, 0, 1, 1) else StateColor(1, 0, 0, 1), "counter: " + counter)

  def PrintIt(s: String) = println(s)
}

object PingActor {
  val props: Props = Props[PingActor]
  case object Initialize
  case class PingMessage(text: String)
  case object Dummy1
  case object Dummy2
}