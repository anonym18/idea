package akka.dispatch.util

import akka.actor.ActorRef
import akka.dispatch.io.ReplayTraceRW
import akka.dispatch.state.{EventHistory, ExecutionState}
import debugger.protocol.Events.EventJsonFormat
import debugger.protocol.{MessageReceived, MessageSent}

object DispatcherUtils {

  private val systemActorPaths = Set("", "user", "system")
  private val debuggerActorNames = Set("DebuggingDispatcherHelperActor", "DebuggingDispatcherTCPInvoker", "DebuggingDispatcherTCPClientActor")
  /**
    * @param  actorRef an ActorRef
    * @return true if the actor is created and used by the ActorSystem
    */
  def isSystemActor(actorRef: ActorRef): Boolean = actorRef.path.elements match {
    case p :: Nil if systemActorPaths contains p => true
    case "user" :: p :: Nil if debuggerActorNames contains p => true
    case "system" :: _ => true
    case _ => false
  }

  def logInfo(): Unit = {
    FileUtils.printToFile("sendReceiveEvents") { p =>
      ExecutionState.getAllEvents.map(EventHistory.convertToIOEvent)
        .filter(e => e.isInstanceOf[MessageReceived] || e.isInstanceOf[MessageSent])
        .map(EventJsonFormat.write)
        .foreach(p.println)
    }

    FileUtils.printToFile("allEvents") { p =>
      ExecutionState.getAllEvents.foreach(p.println)
    }

    FileUtils.printToFile("dependencies") { p => {
      ExecutionState.getAllPredecessorsWithDepType.foreach(x => {
        val dependencies = x._2
        p.print(x._1 + " -> ")
        dependencies.foreach(d => p.print(d._2 + " "))
        p.println()
      })
      ReplayTraceRW.saveEventsToFile("replayEvents", ExecutionState.getReceiveDropEvents)

    }}
  }

}

