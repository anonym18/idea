package app.pingpong

import akka.actor.{Actor, ActorLogging, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import akka.dispatch.UnhandledExceptionLogging
import debugger.protocol.{State, StateColor}

class PongActor extends Actor with ActorLogging with UnhandledExceptionLogging {
  import PongActor._

  def receive = {
  	case PingActor.PingMessage(text) =>
  	  log.info("In PongActor - received message: {} in {}", text, Thread.currentThread().getName)
      val tokens = text.split(" ")
      if (tokens.size == 2 && tokens(0).equals("ping"))
        sender() ! PongMessage("pong " + tokens(1) )

    case GetInternalActorState => sender() ! getState
  }

  def getState : State = State(self.path.name, StateColor(0, 1, 0, 1), "no locals")
}

object PongActor {
  val props = Props[PongActor]
  case class PongMessage(text: String)
}
