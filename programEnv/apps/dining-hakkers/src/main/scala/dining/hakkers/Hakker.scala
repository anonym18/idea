package dining.hakkers

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

/*
* A hakker is an awesome dude or dudette who either thinks about hacking or has to eat ;-)
*/
class Hakker(name: String, left: ActorRef, right: ActorRef) extends Actor with ActorLogging {

  //import context.dispatcher

  //When a hakker is thinking it can become hungry
  //and try to pick up its chopsticks and eat
  def thinking: Receive = {
    case Eat =>
      context.become(hungry)
      left ! Take(self)
      right ! Take(self)
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 0, 1), "Thinking" )
  }

  //When a hakker is hungry it tries to pick up its chopsticks and eat
  //When it picks one up, it goes into wait for the other
  //If the hakkers first attempt at grabbing a chopstick fails,
  //it starts to wait for the response of the other grab
  def hungry: Receive = {
    case Taken(`left`) =>
      context.become(waiting_for(right,left))
    case Taken(`right`) =>
      context.become(waiting_for(left,right))
    case Busy(chopstick) =>
      context.become(denied_a_chopstick(chopstick))
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1), "Waiting for chopsticks" )
  }

  //When a hakker is waiting for the last chopstick it can either obtain it
  //and start eating, or the other chopstick was busy, and the hakker goes
  //back to think about how he should obtain his chopsticks :-)
  def waiting_for(chopstickToWaitFor: ActorRef, otherChopstick: ActorRef): Receive = {
    case Taken(`chopstickToWaitFor`) =>
      log.info(name + " has picked up " + left.path.name + " and " + right.path.name + " and starts to eat")
      context.become(eating)
      //context.system.scheduler.scheduleOnce(Duration(4, TimeUnit.SECONDS), self, Think)
      self ! Think
    case Busy(chopstick) =>
      context.become(thinking)
      //otherChopstick ! Put(self)   // Seeded bug! Assume the programmer forgot to put the first fork when the second one is denied!
      self ! Eat
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Has: " + otherChopstick.path.name + "\nWaiting for: " + chopstickToWaitFor.path.name)
  }

  //When the results of the other grab comes back,
  //he needs to put it back if he got the other one.
  //Then go back and think and try to grab the chopsticks again
  def denied_a_chopstick(deniedStick: ActorRef): Receive = {
    case Taken(chopstick) =>
      context.become(thinking)
      chopstick ! Put(self)
      self ! Eat
      log.info(self.path.name)
    case Busy(_) =>
      context.become(thinking)
      self ! Eat
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Denied request to: " + deniedStick.path.name)
  }

  //When a hakker is eating, he can decide to start to think,
  //then he puts down his chopsticks and starts to think
  def eating: Receive = {
    case Think =>
      context.become(thinking)
      left ! Put(self)
      right ! Put(self)
      log.info(name + " puts down his chopsticks and starts to think")
      //B context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, Eat) // Removed
      self ! Eat

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 0, 0, 1), "Eating" )
  }

  //All hakkers start in a non-eating state
  def receive: Receive = {
    case Think =>
      log.info(name + " starts to think")
      context.become(thinking)
      //context.system.scheduler.scheduleOnce(Duration(5, TimeUnit.SECONDS), self, Eat) // Removed
      self ! Eat

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 0, 1), "Initialized - Idle")
  }
}

object Hakker {
  def props(name: String, left: ActorRef, right: ActorRef) = Props(new Hakker(name, left, right))
}