package mysterious

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}


class Entity(name: String, left: ActorRef, right: ActorRef) extends Actor with ActorLogging {

  def idle: Receive = {
    case WantToProcess =>
      context.become(wantsToProcess)
      left ! Acquire(self)
      right ! Acquire(self)
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 0, 1), "Idle" )
  }

  def wantsToProcess: Receive = {
    case Acquired(`left`) =>
      context.become(waiting_for(right,left))
    case Acquired(`right`) =>
      context.become(waiting_for(left,right))
    case Busy(resource) =>
      context.become(denied_a_resource(resource))
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 1, 1), "Waiting for resources" )
  }

  def waiting_for(resourceToWaitFor: ActorRef, otherResource: ActorRef): Receive = {
    case Acquired(`resourceToWaitFor`) =>
      log.info(name + " has acquired " + left.path.name + " and " + right.path.name + " and starts to process")
      context.become(processing)
      self ! GoIdle
    case Busy(`resourceToWaitFor`) =>
      context.become(idle)
      //otherResource ! Put(self)   // Seeded bug!
      self ! WantToProcess
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Acquired: " + otherResource.path.name + "\nWaiting for: " + resourceToWaitFor.path.name)
  }

  def denied_a_resource(deniedResource: ActorRef): Receive = {
    case Acquired(resource) =>
      context.become(idle)
      resource ! Release(self)
      self ! WantToProcess
      log.info(self.path.name)
    case Busy(_) =>
      context.become(idle)
      self ! WantToProcess
    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 0, 1),
      "Denied request to: " + deniedResource.path.name)
  }

  def processing: Receive = {
    case GoIdle =>
      context.become(idle)
      left ! Release(self)
      right ! Release(self)
      log.info(name + " releases his resources.")
      self ! WantToProcess

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 0.5f, 0, 1), "Processing" )
  }

  def receive: Receive = {
    case GoIdle =>
      log.info(name + " goes idle")
      context.become(idle)
      self ! WantToProcess

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 0, 1), "Initialized")
  }
}

object Entity {
  def props(name: String, left: ActorRef, right: ActorRef) = Props(new Entity(name, left, right))
}