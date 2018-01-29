package akka.dispatch.time

import akka.actor.Actor
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import akka.dispatch.time.TimerActor.AdvanceTime
import akka.dispatch.util.DispatcherUtils
import debugger.protocol.{State, StateColor}

import scala.concurrent.duration.FiniteDuration


class TimerActor(step: FiniteDuration) extends Actor {
  override def receive: Receive = {
    case AdvanceTime =>
      MockTime.time.advance(step)
      self ! AdvanceTime
    case GetInternalActorState =>
      sender() ! State(self.path.name, StateColor(1, 1, 0, 0), "Elapsed: " + MockTime.time)
    case _ => // do nth
  }
}

object TimerActor {
  case object AdvanceTime
}
