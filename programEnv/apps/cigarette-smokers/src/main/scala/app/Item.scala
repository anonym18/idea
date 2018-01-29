package app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

/*
* An item (paper, tobacco or match) is an actor, it can be taken, and put back
*/
class Item(name: String) extends Actor with ActorLogging {

  def notReleased: Receive = {
    case Release =>
      context.become(available)

    case Take(actor: ActorRef) =>
      actor ! NotReleased(self)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 1, 1), "Not released" )
  }

  def available: Receive = {
    case Take(actor: ActorRef) =>
      context.become(busy(actor))
      actor ! Taken(this.self)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 1, 0, 1), "Available" )
  }

  def busy(takenBy: ActorRef): Receive = {
    case Put(`takenBy`) =>
      context.become(available)

    case Take(actor: ActorRef) =>
      actor ! Busy(this.self)

    case Consumed(`takenBy`) =>
      context.become(notReleased)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 0, 0, 1), "Busy" )

  }

  //Initially, the items are not released (Agent releases the items)
  def receive: Receive = notReleased
}

object Item {
  def props(name: String) = Props(new Item(name))
}
