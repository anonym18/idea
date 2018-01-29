package app.toy

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import app.toy.Terminator.ActionDone
import app.toy.Writer.Flush
import debugger.protocol.{State, StateColor}

class Terminator(actionNum: Int, writer: ActorRef) extends Actor with ActorLogging{
  var curActions = actionNum

  def receive = {
    case ActionDone =>
      curActions = curActions - 1
      log.info("Terminator received ActionDone message")
      if (curActions == 0) writer ! Flush

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0.5f, 0.5f, 0.5f, 0.5f), "no locals")
  }
}

object Terminator {
  def props(actionNum: Int, writer: ActorRef) = Props(new Terminator(actionNum, writer: ActorRef))
  case object ActionDone
}


