package app

import akka.actor.{Actor, ActorLogging, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import akka.dispatch.UnhandledExceptionLogging
import debugger.protocol.{State, StateColor}

class Customer(customerName: String) extends Actor with ActorLogging with UnhandledExceptionLogging {

  def receive: Receive = {
    case EntersShop =>
      log.info(customerName + " - Enters shop")
      BarberShop.barberChair ! WantsBarberChair(self)
      context.become(checkingBarberChair)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Initialized")

    case _ => log.info(" - Received unknown message ")
  }

  def checkingBarberChair: Receive = {
    case OnBarberChair =>
      log.info(customerName + " - On barber chair")
      context.become(onBarberChair)

    case BarberChairOccupied =>
      log.info(customerName + " - Goes to the waiting room")
      BarberShop.waitingRoom ! GoesToWaitingRoom(self)
      context.become(waiting)

    case HairCutDone =>
      throw new Exception("Hair cut while the customer is not on the barber chair") // hits the bug!

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Checking barber chair")
  }

  def onBarberChair: Receive = {
    case HairCutDone =>
      log.info(customerName + " - Exiting shop")
      BarberShop.barberChair ! FreesBarberChair(self)
      //context.stop(self)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "On barber chair")
  }

  def waiting: Receive = {
    case InWaitingRoom =>
      log.info(customerName + " - is in the waiting room")

    case WaitingRoomFull =>
      log.info(customerName + " - Waiting room full, Exiting shop")
      context.stop(self)

    case GoToBarberChair =>
      log.info(customerName + " - Goes to the barber chair")
      BarberShop.barberChair ! WantsBarberChair(self)
      context.become(checkingBarberChair)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Waiting")
  }
}

object Customer {
  def props(name: String) = Props(new Customer(name))
}