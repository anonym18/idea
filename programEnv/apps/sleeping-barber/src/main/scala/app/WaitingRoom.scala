package app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

class WaitingRoom(numChairs: Int) extends Actor with ActorLogging {

  private var waitingCustomers: List[ActorRef] = List()

  override def receive: Receive = {
    case GoesToWaitingRoom(customer) if waitingCustomers.size < numChairs =>
      println("==  " + customer + " waits in waiting room")
      waitingCustomers = waitingCustomers :+ customer
      customer ! InWaitingRoom

    case GoesToWaitingRoom(customer) =>
      customer ! WaitingRoomFull

    case BarberChairFree if waitingCustomers.nonEmpty =>
      log.info("Free barber chair")
      val customer = waitingCustomers.head
      waitingCustomers = waitingCustomers.tail
      customer ! GoToBarberChair

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 0.5f), "Waiting: " + waitingCustomers)
  }
}

object WaitingRoom {
  def props(numChairs: Int) = Props(new WaitingRoom(numChairs))
}
