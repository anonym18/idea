package akka.dispatch

import java.util.concurrent._

import akka.actor.{ActorCell, ActorInitializationException, ActorRef, InternalActorRef}
import akka.dispatch.io.ReplayTraceRW
import akka.dispatch.state.{EventHistory, ExecutionState}
import akka.dispatch.sysmsg._
import akka.event.Logging._
import com.typesafe.config.Config
import debugger.protocol._
import util.{CmdLineUtils, DispatcherUtils, FileUtils}
import util.FunUtils._

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * An extension of Dispatcher with logging facilities
  */
final class LoggingDispatcher(
                        _configurator:     MessageDispatcherConfigurator,
                        _id:               String,
                        _shutdownTimeout:  FiniteDuration)
  extends Dispatcher(
    _configurator,
    _id,
    Int.MaxValue,
    Duration.Zero,
    LoggingThreadPoolConfig(),
    _shutdownTimeout) {

  private val history: EventHistory = new EventHistory()

  /**
    * Overriden to log the dispatched messages
    * @param receiver receiver of the intercepted message
    * @param invocation envelope of the intercepted message
    */
  override def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {
    // println("In dispatch : " + invocation + " " + Thread.currentThread().getName)

    if(!DispatcherUtils.isSystemActor(receiver.self)) {
      invocation match {
        case Envelope(Error(_, _, _, _), _)
             | Envelope(Warning(_, _, _), _)
             | Envelope(Info(_, _, _), _)
             | Envelope(Debug(_, _, _), _) =>
          printLog(CmdLineUtils.LOG_INFO, "Msg delivered to: " + receiver.self + " " + invocation + " " + Thread.currentThread().getName)

        // msg is of interest
        case _ =>
          printLog(CmdLineUtils.LOG_INFO, "Msg delivered to: " + receiver.self + " " + invocation + " " + Thread.currentThread().getName)
          history.addEvent(MessageSent(receiver.self.path.name, invocation.sender.path.name, invocation.message.toString, Settings.actorSettings(receiver.self.path.name).isSuppressed))
          // the receiver mailbox is registered to receive the message
          history.addEvent(MessageReceived(receiver.self.path.name, invocation.sender.path.name, invocation.message.toString, Settings.actorSettings(receiver.self.path.name).isSuppressed))
      }
    }
    else {
      printLog(CmdLineUtils.LOG_INFO, "Msg delivered to: " + receiver.self + " " + invocation + " " + Thread.currentThread().getName)
    }

    val mbox = receiver.mailbox
    mbox.enqueue(receiver.self, invocation)
    registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
  }

  /**
    * Overriden to update the actorMap with termination and other system messages
    * @param receiver receiver of the system message
    * @param invocation the dispatched system message
    */
  override def systemDispatch(receiver: ActorCell, invocation: SystemMessage): Unit = {
    printLog(CmdLineUtils.LOG_DEBUG, "System msg: " + invocation + " delivered to: " + receiver.self)

    // run the updates on the thread pool thread
    executorService execute updateActorMapWithSystemMessage(receiver, invocation)

    // System messages are processed each time when the mailbox is executed, their execution are not controlled
    val mbox = receiver.mailbox
    mbox.systemEnqueue(receiver.self, invocation)
    registerForExecution(mbox, hasMessageHint = false, hasSystemMessageHint = true) // terminating an actor..
  }

  def updateActorMapWithSystemMessage(receiver: ActorCell, invocation: SystemMessage): Runnable = toRunnable(() => {
    /**
      * Create messages are directly enqueued into the mailbox, without calling systemDispatch
      * IMPORTANT: Run here before registerForExecution so that actorMessagesMap is updated before terminated actor is deleted
      */
      invocation match {
        case Create(failure: Option[ActorInitializationException]) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Create by failure: " + failure)
        case Recreate(cause: Throwable) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Recreate by cause: " + cause)
        case Suspend() => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Suspend")
        case Resume(causedByFailure: Throwable) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Resume by failure: " + causedByFailure)
        case Terminate() =>
          printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg terminates: " + receiver.self)
        case Supervise(child: ActorRef, async: Boolean) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Supervise. Child: " + child)
        case Watch(watchee: InternalActorRef, watcher: InternalActorRef) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Watch. Watchee: " + watchee + " Watcher: " + watcher)
        case Unwatch(watchee: ActorRef, watcher: ActorRef) => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: Unwatch. Watchee: " + watchee + " Watcher: " + watcher)
        case NoMessage => printLog(CmdLineUtils.LOG_DEBUG, "Handling system msg: NoMessage")
        case _ => // do not track the other system messages for now
      }
  })

  /**
    * Overriden to add the actors with the created mailbox into the ActorMap
    */
  override def createMailbox(actor: akka.actor.Cell, mailboxType: MailboxType): Mailbox = {
    printLog(CmdLineUtils.LOG_INFO, "Created mailbox for: " + actor.self  + " in thread: " + Thread.currentThread().getName)
    new Mailbox(mailboxType.create(Some(actor.self), Some(actor.system))) with DefaultSystemMessageQueue
  }

  /**
    * Overriden to output the recorded events
    */
  override def shutdown: Unit = {
    printLog(CmdLineUtils.LOG_INFO, "Shutting down.. ")

    FileUtils.printToFile("allEvents"){ p => p.println(history.toString) }

    ReplayTraceRW.saveEventsToFile("replayEvents", ExecutionState.getReceiveDropEvents)

    super.shutdown
  }

  def printLog(logType: Int, text: String): Unit = CmdLineUtils.printLog(logType, text)
}

class LoggingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new LoggingDispatcher(
    this,
    config.getString("id"),
    Duration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}



