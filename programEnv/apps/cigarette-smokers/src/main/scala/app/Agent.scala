package app

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

class Agent(tobacco: ActorRef, paper: ActorRef, matches: ActorRef) extends Actor with ActorLogging {
  val r = new scala.util.Random(1234)
  val resources: Map[Int, ActorRef] = (0 to 2).zip(List(tobacco, paper, matches)).toMap

  def receive: Receive = {
    case InvokeAgent  =>
      // release two of the resources
      val num1 = getRandomInt(None, 3)
      val r1 = resources(num1)
      println("Released: " + r1)
      val r2 = resources(getRandomInt(Some(num1), 3))
      println("Released: " + r2)
      r1 ! Release
      r2 ! Release

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(1, 1, 1, 1), "No state" )

  }

  private def getRandomInt(except: Option[Int], max: Int): Int = except match {
    case None => r.nextInt(max)
    case Some(x) => {
      val next = r.nextInt(max)
      if (next != x) next
      else getRandomInt(Some(x), max)
    }
  }
}

object Agent {
  def props(tobacco: ActorRef, paper: ActorRef, matches: ActorRef) = Props(new Agent(tobacco, paper, matches))
}
