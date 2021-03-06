package app

import akka.actor.{ActorRef, ActorSystem}
import akka.dispatch.DebuggingDispatcher

/**
  *  https://gist.github.com/dholbrook/1280150
  */

object BarberShop extends App {

  val system = ActorSystem("sys")
  val barber = system.actorOf(Barber.props, "Barber")

  val barberChair: ActorRef = system.actorOf(BarberChair.props, "BarberChair")
  val waitingRoom:ActorRef = system.actorOf(WaitingRoom.props(2), "WaitingRoom")

  for (i <- 1 to 3) {
    val c = system.actorOf(Customer.props("Customer " + i), "C" + i)
    c ! EntersShop
  }

  DebuggingDispatcher.setActorSystem(system)
  DebuggingDispatcher.setUp()
}