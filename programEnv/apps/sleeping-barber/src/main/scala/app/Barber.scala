package app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}



class Barber extends Actor with ActorLogging {

  def sleeping: Receive = {
    case CutHair(customer) =>
      log.info("Barber - Started cutting hair of " + customer.path.name)
      context.become(cuttingHair(customer))
      self ! ProcessingDone

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Sleeping")

    case _ => log.info("Barber - Received unknown message")
  }

  def cuttingHair(customer: ActorRef): Receive = {
    case ProcessingDone =>
      log.info("Barber - Finished cutting hair of " + customer.path.name)
      customer ! HairCutDone
      context.become(sleeping)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Cutting hair")

    case _ => log.info("Barber - Received unknown message")
  }

  def receive = sleeping
}

object Barber {
  def props = Props(new Barber())
}