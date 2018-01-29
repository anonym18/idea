package akka.dispatch

import akka.actor.{Actor, ActorLogging}
import debugger.protocol.{Events, Log}

trait UnhandledExceptionLogging {
  self: Actor with ActorLogging =>

  override def preRestart(reason:Throwable, message:Option[Any]){
    this.context.dispatcher match {
      case d: DebuggingDispatcher => d.sendErrResponse(Log(Events.LOG_ERROR, "Unhandled exception: " + reason.toString,
        isSuppressed = Settings.debuggerSettings.logLevel > Events.LOG_WARNING), this.self)
      case _ =>
    }
  }
}
