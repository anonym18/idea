package mysterious

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

class Resource(name: String) extends Actor with ActorLogging {

  def acquiredBy(entity: ActorRef): Receive = {
    case Acquire(otherEntity) =>
      otherEntity ! Busy(self)
    case Release(`entity`) =>
      context.become(available)
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 0, 0, 1), "Acquired by: " + entity.path.name)
  }

  def available: Receive = {
    case Acquire(entity) =>
      context.become(acquiredBy(entity))
      entity ! Acquired(self)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 1, 0, 1), "Available" )
  }

  def receive: Receive = available
}

object Resource {
  def props(name: String) = Props(new Resource(name))
}
