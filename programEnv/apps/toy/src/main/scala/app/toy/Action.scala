package app.toy

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import app.toy.Action.Execute
import app.toy.Terminator.ActionDone
import app.toy.Writer.Write
import debugger.protocol.{State, StateColor}

class Action(name: String, terminator: ActorRef, writer: ActorRef) extends Actor with ActorLogging{

  def receive = {
    case Execute =>
      log.info("Terminator received ActionDone message")
      writer ! Write("msg 1 to write")
      writer ! Write("msg 2 to write")
      terminator ! ActionDone

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0.5f, 0.5f, 0.5f, 0.5f), "no locals")
  }
}

object Action {
  def props(name: String, terminator: ActorRef, writer: ActorRef) = Props(new Action(name, terminator, writer))
  case object Execute
}
