package dining.hakkers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

/*
* A Chopstick is an actor, it can be taken, and put back
*/
class Chopstick(name: String) extends Actor with ActorLogging {

  //When a Chopstick is taken by a hakker
  //It will refuse to be taken by other hakkers
  //But the owning hakker can put it back
  def takenBy(hakker: ActorRef): Receive = {
    case Take(otherHakker) =>
      otherHakker ! Busy(self)
    case Put(`hakker`) =>
      context.become(available)
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 0, 0, 1), "Taken by: " + hakker.path.name)
  }

  //When a Chopstick is available, it can be taken by a hakker
  def available: Receive = {
    case Take(hakker) =>
      context.become(takenBy(hakker))
      hakker ! Taken(self)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 1, 0, 1), "Available" )
  }

  //A Chopstick begins its existence as available
  def receive: Receive = available
}

object Chopstick {
  def props(name: String) = Props(new Chopstick(name))
}
