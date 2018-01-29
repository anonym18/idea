package app

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

import scala.concurrent.duration.Duration
import scala.util.Random

/*
* A smoker has either tobacco, paper or match
* It receives the references of the two items as parameter
*/
class Smoker(item1: ActorRef, item2: ActorRef, agent: ActorRef) extends Actor with ActorLogging {
  import context.dispatcher

  def idle: Receive = {
    case Smoke =>
      //val rand = Random.nextInt(1000)
      //Thread.sleep(rand)
      context.become(wantsToSmoke)
      item1 ! Take(self)
      item2 ! Take(self)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 1, 1), "Idle" )
  }

  def wantsToSmoke(): Receive = {
    case NotReleased(`item1`) => // one of the two resources is not released by the agent
      context.become(oneItemNotReleased(item1, item2))

    case NotReleased(`item2`) =>
      context.become(oneItemNotReleased(item2, item1))

    case Taken(`item1`) =>
      context.become(waiting_for(item2,item1))

    case Taken(`item2`) =>
      context.become(waiting_for(item1,item2))

    case Busy(item) =>
      context.become(denied_an_item(item))

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Waiting for: " + item1.path.name + " and " + item2.path.name)
  }

  def oneItemNotReleased(notReleasedItem: ActorRef, otherItem: ActorRef): Receive = {
    case NotReleased(item) =>
      context.become(idle)
      self ! Smoke

    case Taken(item) =>
      // item ! Put(self) // seeded bug!
      context.become(idle)
      self ! Smoke

    case Busy(item) =>
      context.become(idle)
      self ! Smoke

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Not released: " + notReleasedItem.path.name + "\nWaiting for: " + otherItem.path.name)
  }

  def waiting_for(itemToWaitFor: ActorRef, otherItem: ActorRef): Receive = {
    case NotReleased(item) =>
      otherItem ! Put(self)
      context.become(idle)
      self ! Smoke

    case Taken(`itemToWaitFor`) =>
      log.info(Console.RED + self.path.name + " has picked up " + item1.path.name + " and " + item2.path.name + " and starts to smoke" + Console.RESET)
      context.become(smoking)
      self ! FinishSmoking

    case Busy(`itemToWaitFor`) =>
      context.become(idle)
      otherItem ! Put(self)
      self ! Smoke

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Has: " + otherItem.path.name + "\nWaiting for: " + itemToWaitFor.path.name)
  }

  def denied_an_item(deniedItem: ActorRef): Receive = {
    case NotReleased(item) =>
      context.become(idle)
      self ! Smoke

    case Taken(item) =>
      context.become(idle)
      item ! Put(self)
      self ! Smoke

    case Busy(_) =>
      context.become(idle)
      self ! Smoke

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Denied request to: " + deniedItem.path.name)
  }

  def smoking: Receive = {
    case FinishSmoking =>
      context.become(idle)
      item1 ! Consumed(self)
      item2 ! Consumed(self)
      log.info(self.path.name + " finishes smoking and puts down the items")
      agent ! InvokeAgent
      self ! Smoke

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 0, 1), "Smoking" )
  }

  def receive: Receive = idle
}

object Smoker {
  def props(item1: ActorRef, item2: ActorRef, agent: ActorRef) = Props(new Smoker(item1, item2, agent))
}