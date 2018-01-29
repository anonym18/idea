package app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

class BarberChair extends Actor with ActorLogging {
  def free: Receive = {
    case WantsBarberChair(c) =>
      log.info("Customer " + c + " sits in the barberchair.")
      c ! OnBarberChair
      // The barber cuts hair and sends CutDone to the customer concurrent to OnBarberChair message
      // The Customer throws an exception if the hair is cut before he sits on the barber chair
      BarberShop.barber ! CutHair(c)
      context.become(occupiedBy(c))

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Free")

  }

  def occupiedBy(customer: ActorRef): Receive = {
    case WantsBarberChair(c) =>
      log.info("Customer " + c + " cannot sit in the barber chair.")
      c ! BarberChairOccupied

    case FreesBarberChair(c) =>
      log.info("Customer " + c + " frees the barberchair.")
      context.become(free)
      BarberShop.waitingRoom ! BarberChairFree

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Occupied by: " + customer.path.name)

  }

  override def receive: Receive = free
}

object BarberChair {
  def props = Props(new BarberChair())
}