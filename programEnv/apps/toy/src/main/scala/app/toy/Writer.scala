package app.toy

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

import scala.collection.mutable.ListBuffer

class Writer extends Actor with ActorLogging {
  import Writer._

  var results: ListBuffer[String] = ListBuffer()

  def uninitialized: Receive = {
    case Init =>
      context.become(initialized)
      sender() ! "Initialized"

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0.5f, 0.5f, 0.5f, 0.5f), "uninitialized")

  }

  def initialized: Receive = {
    case Write(result: String) =>
      results.append(result)
    case Flush =>
      writeToExternal(results.toList)
      results = null
      sender ! Flushed
      context.become(uninitialized)

    case GetInternalActorState => sender() ! State(self.path.name, StateColor(0.5f, 0.5f, 0.5f, 0.5f), "initialized")

  }

  def receive = uninitialized

  def writeToExternal(result: List[String]) = {}
}

object Writer {
  val props: Props = Props(new Writer)
  case class Write(msg: String)
  case object Init
  case object Flush
  case object Flushed
}
