package akka.dispatch

import java.lang.reflect.Field
import java.util.concurrent._

import akka.actor.{Actor, ActorCell, ActorInitializationException, ActorRef, ActorSystem, Cell, InternalActorRef, Props}
import akka.dispatch.io._
import akka.dispatch.state.ExecutionState
import akka.dispatch.time.TimerActor.AdvanceTime
import akka.dispatch.sysmsg.{NoMessage, _}
import akka.event.Logging._
import akka.io.Tcp
import akka.io.Tcp.{apply => _, _}
import akka.pattern.{PromiseActorRef, ask}
import akka.util.Timeout
import com.typesafe.config.Config
import time.TimerActor
import debugger.protocol._
import util.{CmdLineUtils, DispatcherUtils, ReflectionUtils}
import util.FunUtils._

import scala.collection.mutable
import scala.collection.mutable.{Set => MSet}
import scala.concurrent.Await
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * The following messages used to communicate the user requests to the dispatcher
  * These messages are not intercepted or delivered to any actor
  * They are used to invoke associated handler methods async on the dispatcher thread
  */
sealed trait DispatcherMsg

/**
  * To handle ActionRequest Init
  */
private case object InitDispatcher extends DispatcherMsg

/**
  * To handle ActionRequest End
  */
private case object EndDispatcher extends DispatcherMsg

/**
  * To handle StateRequest
  */
private case class AskActorState(actor: Cell, toGet: Boolean) extends DispatcherMsg

/**
  * To handle ActionRequest to dispatch to a given actor
  */
private case class DispatchToActor(actor: Cell) extends DispatcherMsg

/**
  * To handle ActionRequest to dispatch to the next actor in the execution trace
  */
private case object DispatchToNextActor extends DispatcherMsg

/**
  * To handle DropMsgRequest to drop the message at the head of the actor msg list
  */
private case class DropActorMsg(actor: Cell) extends DispatcherMsg

/**
  * To handle TagActorRequest
  */
private case class TagActor(actor: Cell, toTag: Boolean) extends DispatcherMsg

/**
  * To handle SuppressRequest
  */
private case class SuppressActor(actor: Cell, toSuppress: Boolean) extends DispatcherMsg

/**
  * To send a warning/error response appears while resolving the request
  */
private case class SendResponse(response: QueryResponse) extends DispatcherMsg

/**
  * To handle StepRequest
  */
private case class GotoStep(targetStep: Int) extends DispatcherMsg

/**
  * For sending log messages to the dispatcher generated while reading a user Request
  */
private case class LogMsg(logType: Int, text: String) extends DispatcherMsg

object DebuggingDispatcher {
  /**
    * The dispatcher gets user inputs via a connection to a TCP server
    */
  val ioProvider: IOProvider = NetworkIOProvider

  val logLevel: Int = Settings.debuggerSettings.logLevel

  /**
    * The following methods are called by QueryRequestHandler to process user requests
    * These requests are sent to the dispatcher and handled by the dispatcher thread async
    */
  /**
    * Called when the user requests the state of an actor
    * Asks the actor its actor state, waits for and returns the result of the future
    *
    * @param actor actorCell
    * @return the actor state as a String
    */
  def queryActorState(actor: Cell, toGet: Boolean): Unit = {
    sendToDispatcher(AskActorState(actor, toGet))
  }

  /**
    * Called when the user requests the state of an actor
    * Asks the actor its actor state, waits for and returns the result of the future
    *
    * @param actorName - ActorRef value as String
    * @return the actor state as a String
    */
  def queryActorState(actorName: String, toGet: Boolean): Unit = {
    ExecutionState.getActor(actorName) match {
      case Some(actor) => queryActorState(actor.asInstanceOf[ActorCell], toGet: Boolean)
      case None if actorName.equals("deadLetters") =>
        sendToDispatcher(SendResponse(ActionResponse(noStepNum, List(), List(State("deadLetters", StateColor(0, 0, 0, 1), "No state")))))
      case None => printLog(CmdLineUtils.LOG_ERROR, "Cannot query actor state: " + actorName + " No such actor.")
    }
  }

  /**
    * Called when the user requests to dispatch the next message to a given actor
    *
    * @param actor cell which receives the next message
    */
  def dispatchToActor(actor: Cell): Unit = {
    sendToDispatcher(DispatchToActor(actor))
  }

  /**
    * Called when the user requests to dispatch the next message to a given actor
    *
    * @param actorName actor name which receives the next message
    */
  def dispatchToActor(actorName: String): Unit = {
    val actor = ExecutionState.getActor(actorName)
    actor match {
      case Some(a) => dispatchToActor(a)
      case None => printLog(CmdLineUtils.LOG_ERROR, "Cannot dispatch to actor: " + actorName + " No such actor.")
    }
  }

  /**
    * Called when the user requests to step next in the replayed execution trace
    * Dispatch message to the next actor in replayed trace
    */
  def dispatchToNextActor(runIfTagged: Boolean = true): Unit = {
    sendToDispatcher(DispatchToNextActor)
  }

  /**
    * Called when the user requests to drop the head message of the given actor
    *
    * @param actor cell whose head message will be dropped
    */
  def dropActorMsg(actor: Cell): Unit = {
    sendToDispatcher(DropActorMsg(actor))
  }

  /**
    * Called when the user requests to drop the head message of the given actor
    *
    * @param actorName cell whose head message will be dropped
    */
  def dropActorMsg(actorName: String): Unit = {
    val actor = ExecutionState.getActor(actorName)
    actor match {
      case Some(a) => dropActorMsg(a)
      case None => printLog(CmdLineUtils.LOG_ERROR, "Cannot drop msg from the actor: " + actorName + " No such actor.")
    }
  }

  /**
    * Called when the user requests to tag an actor
    * This causes the program to run until the tagged actor is sent a message
    */
  def tagActor(actor: Cell, toTag: Boolean): Unit = {
    sendToDispatcher(TagActor(actor, toTag))
  }

  /**
    * Called when the user requests to tag an actor
    * This causes the program to run until the tagged actor is sent a message
    */
  def suppressActor(actorName: String, toSuppress: Boolean): Unit = {
    val actor = ExecutionState.getActor(actorName)
    actor match {
      case Some(a) => suppressActor(a, toSuppress)
      case None => printLog(CmdLineUtils.LOG_ERROR, "Cannot tag actor: " + actorName + " No such actor.")
    }
  }

  /**
    * Called when the user requests to suppress messages of an actor
    * The suppressed messages are not visualized
    */
  def suppressActor(actor: Cell, toSuppress: Boolean): Unit = {
    sendToDispatcher(SuppressActor(actor, toSuppress))
  }

  /**
    * Called when the user requests to go back to a step in history
    */
  def gotoStep(targetStep: Int): Unit = {
    sendToDispatcher(GotoStep(targetStep))
  }

  /**
    * Called when the user requests to tag an actor
    * This causes the program to run until the tagged actor is sent a message
    */
  def tagActor(actorName: String, toTag: Boolean): Unit = {
    val actor = ExecutionState.getActor(actorName)
    actor match {
      case Some(a) => tagActor(a, toTag)
      case None => printLog(CmdLineUtils.LOG_ERROR, "Cannot tag actor: " + actorName + " No such actor.")
    }
  }

  /**
    * Called when the user requests to initiate the dispatcher
    * Send the initial list of actor events to the user
    */
  def initiateDispatcher(): Unit = sendToDispatcher(InitDispatcher)

  /**
    * Called when the user requests to terminate the program
    */
  def terminateDispatcher(): Unit = sendToDispatcher(EndDispatcher)

  /**
    * Called when the user requests to visualize the actors in a certain topography
    */
  def sendTopography(): Unit =
  Settings.visualizationSettings.topographyType match {
    case Some(topography) if Settings.visualizationSettings.topographyActorList.isDefined =>
      ioProvider.putResponse(TopographyResponse(topography, Settings.visualizationSettings.topographyActorList.get))
    case _ =>
      val log = Log(Events.LOG_WARNING, "Visualization topography is not specified in the configuration file.", isSuppressed = logLevel > Events.LOG_WARNING)
      ioProvider.putResponse(ActionResponse(noStepNum, List(log), List()))
  }

  /**
    * The following variables will be filled when the Dispatcher is set up with the actor system parameter
    */
  var actorSystem: Option[ActorSystem] = None
  var helperActor: Option[ActorRef] = None
  var timerActor: Option[ActorRef] = None

  def setActorSystem(system: ActorSystem): Unit = if (system.dispatcher.isInstanceOf[DebuggingDispatcher]) actorSystem = Some(system)

  /**
    * Enables dispatcher to deliver messages to the actors
    * To be called by the app when it is done with the actor creation/initialization
    */
  def setUp(): Unit = actorSystem match {
    case Some(system) if system.dispatcher.isInstanceOf[DebuggingDispatcher] => // check to prevent initializing ioProvider while using another Dispatcher type
      actorSystem = Some(system)
      helperActor = Some(system.actorOf(Props(new Actor() {
        override def receive: Receive = Actor.emptyBehavior
      }), "DebuggingDispatcherHelperActor"))

      // create TimerActor only if the user uses virtual time (e.g. scheduler.schedule methods) in his program
      if(Settings.debuggerSettings.useTimer) {
        val timer = system.actorOf(Props(new TimerActor(Settings.debuggerSettings.timeStep)), "Timer")
        timerActor = Some(timer)
        timer ! AdvanceTime
      }

      // server connection etc to communicate to the debugger UI (e.g. VR end)
      ioProvider.setUp(system)
    case _ => // do nth
  }

  def sendToDispatcher(msg: Any): Unit = helperActor match {
    case Some(actor) => actor ! msg
    case None => printLog(CmdLineUtils.LOG_ERROR, "Cannot send to the dispatcher, no helper actor is created")
  }

  def printLog(logType: Int, text: String): Unit = CmdLineUtils.printLog(logType, text)

  //def listOfActorsByCell: List[(Cell, List[Envelope])] = actorMessagesMap.toListWithActorCell

  //def listOfActorsByRef: List[(ActorRef, List[Envelope])] = actorMessagesMap.toListWithActorRef

  //def listOfActorsByName: List[(String, List[Envelope])] = actorMessagesMap.toListWithActorPath

  /**
    * Used to get the internal state of an actor
    * The user actors must be implemented to accept this message and to respond this message with its internal state
    */
  case object GetInternalActorState
  private val noStepNum: Int = -1
}


/**
  * An extension of Dispatcher with logging facilities
  */
final class DebuggingDispatcher(_configurator: MessageDispatcherConfigurator,
                                _id: String,
                                _shutdownTimeout: FiniteDuration)
  extends Dispatcher(
    _configurator,
    _id,
    Int.MaxValue,
    Duration.Zero,
    LoggingThreadPoolConfig(),
    _shutdownTimeout) {

  import DebuggingDispatcher._

  /**
    * The actors whose state is watched (sent to the user each time it changes)
    */
  private val replayEvents: ReplayTraceRW = new ReplayTraceRW()
  /**
    * The actors whose state is watched (sent to the user each time it changes)
    */
  private val watchedActors: MSet[Cell] = MSet[Cell]()
  /**
    * The program executes uninterruptedly until a tagged actor receives a message
    */
  private val taggedActors: MSet[Cell] = MSet[Cell]()
  /**
    * The suppressed actors are not visualized
    */
  private val suppressedActors: MSet[ActorRef] = MSet[ActorRef]() //todo read from config file initially

  private var breakpointReached: Boolean = false

  /**
    * Handler methods run synchronously by the dispatcher to handle requests
    */
  private def handleInitiate(): ActionResponse = {
    if(ExecutionState.isInInitialStep && !ExecutionState.isCurrentStepInHistory) {
      val actorStates = ExecutionState.getAllActors.map(actor => getActorState(actor)).toSet
      ActionResponse(ExecutionState.getCurrentStep, ExecutionState.endCurrentStep(actorStates), List())
    } else {
      printLog(CmdLineUtils.LOG_WARNING, "Initiate request while already initiated.")
      ActionResponse(noStepNum, List(Log(Events.LOG_WARNING, "Initiate request while already initiated.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
    }
  }

  private def handleTerminate: Any = actorSystem match {
    case Some(system) =>
      DispatcherUtils.logInfo()
      system.terminate

    case None => printLog(CmdLineUtils.LOG_ERROR, "Cannot terminate")
  }

  private def handleAskActorState(actor: Cell, toGet: Boolean): ActionResponse = {
    if(toGet) {
      watchedActors.add(actor)
      val state = getActorState(actor)
      ActionResponse(noStepNum, List(), List(state))
    } else {
      if (watchedActors.contains(actor))
        watchedActors.remove(actor)
      ActionResponse(noStepNum, List(), List())
    }
  }

  implicit val timeout: Timeout = Timeout(Settings.debuggerSettings.askTimeOutMsec, TimeUnit.MILLISECONDS)

  private def getActorState(actor: Cell): State =
  {
    if(Settings.debuggerSettings.actorStateBy.equals("message"))
      actorStateByAsk(actor)
    else if(Settings.debuggerSettings.actorStateBy.equals("reflection"))
      actorStateByReflection(actor)
    else
      State(actor.self.path.name, StateColor(1, 1, 1, 1), "No state configured")
  }

  private def actorStateByAsk(actor: Cell): State =
    try{
      val future = actor.self ? GetInternalActorState
      Await.result(future, Duration.create(Settings.debuggerSettings.askTimeOutMsec, TimeUnit.MILLISECONDS)).asInstanceOf[State]
    } catch {
      case e: Exception =>
        printLog(CmdLineUtils.LOG_ERROR, "The actor " + actor.self.path.name + " does not respond its state when received GetInternalActorState message")
        State(actor.self.path.name, StateColor(1, 1, 1, 1), "No state received")
    }

  private def actorStateByReflection(actor: Cell): State = {
    val stateString: mutable.StringBuilder = new StringBuilder()

    def getFieldShortName(s: Field): String = s.getName.split("\\$\\$").last

    if(actor.isTerminated) {
      return State(actor.self.path.name, StateColor(1, 1, 1, 1), "terminated")
    }

    // get the name of the behavior function of type Actor.Receive
    var behaviorStackField = actor.asInstanceOf[ActorCell].getClass.getDeclaredField("behaviorStack")
    behaviorStackField.setAccessible(true)

    val actorBeh = behaviorStackField.get(actor).asInstanceOf[List[Actor.Receive]]
    val behaviorName = actorBeh.head.getClass.toString.split("\\.").last
    stateString.append("State: " + behaviorName + "\n")

    // get the actor fields
    val actorInstance = actor.asInstanceOf[ActorCell].actor
    val fields: Array[Field] = actorInstance.getClass.getDeclaredFields
      .filter(x => !getFieldShortName(x).equals("_log") && !getFieldShortName(x).equals("context") && !getFieldShortName(x).equals("self"))

    fields.foreach(x => x.setAccessible(true))
    fields.foreach(x => { x.get(actorInstance) match {
      case aa: ActorRef => stateString.append(getFieldShortName(x) + ": " + aa.path.name + "\n")
      case _ => stateString.append(getFieldShortName(x) + ": " + x.get(actorInstance) + "\n")
    }
    })

    State(actor.self.path.name, StateColor(1, 1, 1, 1), stateString.toString)
  }

  private def getAllActorStates: List[State] = {
    def helper(actors: List[Cell], acc: List[State]): List[State] = actors match {
      case Nil => acc
      case x :: xs => getActorState(x) :: helper(xs, acc)
    }

    helper(ExecutionState.getAllActors.toList, List())
  }

  private def handleDispatchToActor(actor: Cell): ActionResponse = {
    // Cannot dispatch to a selected actor if the history of execution is being replayed
    if(ExecutionState.isCurrentStepInHistory) {
      printLog(CmdLineUtils.LOG_ERROR, "Cannot select next message in HISTORY.")
      ActionResponse(noStepNum, List(Log(Events.LOG_ERROR, "Cannot select next message in HISTORY.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
    } else {
      ExecutionState.receiveMessage(actor, suppressedActors.contains(actor.self)) match {
        case Some(envelope) =>
          dispatchEnvelope(actor, envelope)
          // while processing the message, ProgramEvents is added recently sent messages, etc
          // if the actor is destroyed do not store/send its state
          val receivingActorState = getActorState(actor)
          if (watchedActors.contains(actor))
            ActionResponse(ExecutionState.getCurrentStep, ExecutionState.endCurrentStep(Set(receivingActorState)), List(receivingActorState))
          else
            ActionResponse(ExecutionState.getCurrentStep, ExecutionState.endCurrentStep(Set(receivingActorState)), List())

        case None =>
          printLog(CmdLineUtils.LOG_WARNING, "Cannot dispatch the message.")
          ActionResponse(noStepNum, List(Log(Events.LOG_WARNING, "Cannot dispatch the message..", isSuppressed = logLevel > Events.LOG_WARNING)), List())
      }
    }
  }

  private def dispatchMessage(receiver: Cell, senderName: String, message: String): ActionResponse = {
  val msgToDispatch =
    if(Settings.debuggerSettings.matchMsgContent) // false if not set in the conf file
      ExecutionState.receiveMessage(receiver, senderName, message, suppressedActors.contains(receiver.self))
    else
      ExecutionState.receiveMessage(receiver, suppressedActors.contains(receiver.self))

    msgToDispatch match {
      case Some(envelope) =>
        dispatchEnvelope(receiver, envelope)
        // while processing the message, ProgramEvents is added recently sent messages, etc
        val receivingActorState = getActorState(receiver)
        if (watchedActors.contains(receiver))
          ActionResponse(ExecutionState.getCurrentStep, ExecutionState.endCurrentStep(Set(receivingActorState)), List(receivingActorState))
        else
          ActionResponse(ExecutionState.getCurrentStep, ExecutionState.endCurrentStep(Set(receivingActorState)), List())

      case None =>
        printLog(CmdLineUtils.LOG_WARNING, "Message not found. Cannot dispatch to actor.")
        ActionResponse(noStepNum, List(Log(Events.LOG_WARNING, "Message not found. Cannot dispatch to actor.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
    }
  }

  /**
    * Dispatches the envelope to the given receiver
    * (The receiver and the envelope objects are already checked before calling this function)
    */
  private def dispatchEnvelope(receiver: Cell, envelope: Envelope): Unit = {
    // handle the actor message synchronously
    val actor = receiver.asInstanceOf[ActorCell]
    val mbox = actor.mailbox
    mbox.enqueue(actor.self, envelope)
    processMailbox(mbox)
  }

  private def handleDropMessage(receiver: Cell, senderName: String, message: String): ActionResponse = {
    // Cannot drop msg from a selected actor if the history of execution is being replayed
    if(ExecutionState.isCurrentStepInHistory) {
      printLog(CmdLineUtils.LOG_ERROR, "Cannot drop message in HISTORY.")
      ActionResponse(noStepNum, List(Log(Events.LOG_ERROR, "Cannot drop message in HISTORY.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
    } else {
      ExecutionState.dropMessage(receiver, senderName, message, suppressedActors.contains(receiver.self)) match {
        case Some(envelope) =>
          printLog(CmdLineUtils.LOG_WARNING, "Dropped message: " + envelope)
          ActionResponse(ExecutionState.getCurrentStep, ExecutionState.endCurrentStep(Set()), List())
        case None =>
          printLog(CmdLineUtils.LOG_WARNING, "Cannot drop message.")
          ActionResponse(noStepNum, List(Log(Events.LOG_WARNING, "Cannot drop message.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
      }
    }
  }

  private def handleDispatchToNextActor(): QueryResponse = {
    // If the history of execution is being replayed, go forward a step in history
    if(ExecutionState.isCurrentStepInHistory) {
      val step = ExecutionState.getCurrentStep
      val (events, states) = ExecutionState.goFwInHistory // updates the step to the next step (to be filled with actions)
      ActionResponse(step, events, states.filter(state => isWatchedActor(state.actorId)))
    } else {
      if (replayEvents.hasNext) {
        val event = replayEvents.peek // return an event
        event match {
          // check if the replay event is the next event in the actorMap
          // (if not, find and dispatch this message)
          case MessageReceived(receiverId, senderId, message, _) =>
            val actorName = receiverId
            ExecutionState.getActor(actorName) match {
              case Some(actor) if taggedActors.contains(actor) && !breakpointReached =>
                printLog(CmdLineUtils.LOG_INFO, "Next receiving actor is tagged: " + actorName)
                breakpointReached = true
                TagReachedResponse(receiverId)
              case Some(actor) if taggedActors.contains(actor) && breakpointReached =>
                printLog(CmdLineUtils.LOG_INFO, "Over breakpoint, next receiving actor in trace: " + actorName)
                breakpointReached = false
                replayEvents.consume()
                dispatchMessage(actor, senderId, message)
              case Some(actor) =>
                printLog(CmdLineUtils.LOG_INFO, "Next receiving actor in trace: " + actorName)
                replayEvents.consume()
                dispatchMessage(actor, senderId, message)
              case None =>
                printLog(CmdLineUtils.LOG_ERROR, "The actor " + actorName + " in trace is not created.")
                ActionResponse(noStepNum, List(Log(Events.LOG_ERROR, "The actor " + actorName + " in trace is not created.",
                  isSuppressed = logLevel > Events.LOG_ERROR)), List())
            }
          case MessageDropped(receiverId, senderId, message, _) =>
            val actorName = receiverId
            ExecutionState.getActor(actorName) match {
              case Some(actor) if taggedActors.contains(actor) && !breakpointReached =>
                printLog(CmdLineUtils.LOG_INFO, "Next actor is tagged: " + actorName)
                breakpointReached = true
                TagReachedResponse(receiverId)
              case Some(actor) if taggedActors.contains(actor) && breakpointReached =>
                printLog(CmdLineUtils.LOG_INFO, "Over breakpoint, next actor in trace: " + actorName)
                breakpointReached = false
                replayEvents.consume()
                handleDropMessage(actor, senderId, message)
              case Some(actor) =>
                printLog(CmdLineUtils.LOG_INFO, "Next actor in trace: " + actorName)
                replayEvents.consume()
                handleDropMessage(actor, senderId, message)
              case None =>
                printLog(CmdLineUtils.LOG_ERROR, "The actor " + actorName + "in trace is not created.")
                ActionResponse(noStepNum, List(Log(Events.LOG_ERROR, "The actor " + actorName + "in trace is not created.", isSuppressed = logLevel > Events.LOG_ERROR)), List())
            }

          case _ =>
            printLog(CmdLineUtils.LOG_ERROR, "Not replayed event in the trace: " + event.toString)
            ActionResponse(noStepNum, List(Log(Events.LOG_ERROR, "Not replayed event in the trace: " + event.toString, isSuppressed = logLevel > Events.LOG_ERROR)), List())
        }
      }
      else {
        printLog(CmdLineUtils.LOG_INFO, "End of the trace is reached.")
        EOTResponse
      }
    }
  }

  private def handleDropActorMsg(actor: Cell): ActionResponse = {
    ExecutionState.dropMessage(actor, Settings.actorSettings(actor.self.path.name).isSuppressed) match {
      case Some(envelope) =>
        printLog(CmdLineUtils.LOG_INFO, "Removed head message from: " + actor.self.path + " msg: " + envelope)
        // no state change in any actor (hence buffer is consumed with an empty set of states)
        ActionResponse(ExecutionState.getCurrentStep, ExecutionState.endCurrentStep(Set()), List())

      case None =>
        printLog(CmdLineUtils.LOG_WARNING, "Msg drop from an actor with no messages.")
        ActionResponse(noStepNum, List(Log(Events.LOG_WARNING, "Msg drop from an actor with no messages.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
    }
  }

  /**
    * Sends a response stating the tagged actor
    * @param actor to be tagged/untagged
    * @param toTag true/false if the actor will be tagged/untagged
    */
  private def handleTagActor(actor: Cell, toTag: Boolean): TagResponse = {
    if (toTag) taggedActors.add(actor) else taggedActors.remove(actor)
    TagResponse(actor.self.path.name, toTag)
  }

  /**
    * Sends a response stating the suppressed actor
    * @param actor to be suppressed/unsuppressed
    * @param toSuppress true/false if the actor will be suppressed/unsuppressed
    */
  private def handleSuppressActor(actor: Cell, toSuppress: Boolean): SuppressActorResponse = {
    if (toSuppress) suppressedActors.add(actor.self) else suppressedActors.remove(actor.self)
    SuppressActorResponse(actor.self.path.name, toSuppress)
  }

  private def isWatchedActor(actorName: String): Boolean =
    watchedActors.map(actorCell => actorCell.self.path.name).contains(actorName)

  /**
    * Sends a response listing the events to be visualized for going back to the target step
    * @param targetStep the program step to go back to
    */
  private def handleGoToStepInHistory(targetStep: Int): QueryResponse = {
    // ask for the state just before the target step!
    ExecutionState.goBackInHistory(targetStep - 1) match {
      case Some((events, states)) =>
        taggedActors.clear()
        StepResponse(targetStep - 1, events, states.filter(state => isWatchedActor(state.actorId)))
      case None =>
        printLog(CmdLineUtils.LOG_ERROR, "Target step <=0 or larger than the current step. \nCannot go to the requested step in history.")
        ActionResponse(noStepNum, List(Log(Events.LOG_ERROR, "Target step <=0 or larger than the current step. \nCannot go to the requested step in history.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
    }
  }

  /**
    * Called when an actor has crashed
    */
  def sendErrResponse(event: Log, actor: ActorRef): Unit = {
    // run on the dispatcher thread async
    executorService execute toRunnable(() => {
      ExecutionState.addLogEvent(event)
      ExecutionState.getActor(actor.path.name) match {
        case Some(a) =>
          val actorState = getActorState(a)
          val stepEvents = ExecutionState.endCurrentStep(Set(actorState))
          if (watchedActors.contains(a))
            ioProvider.putResponse(ActionResponse(ExecutionState.getCurrentStep, stepEvents, List(actorState)))
          else
            ioProvider.putResponse(ActionResponse(ExecutionState.getCurrentStep, stepEvents, List()))
        case None =>
          val stepEvents = ExecutionState.endCurrentStep(Set())
          ioProvider.putResponse(ActionResponse(ExecutionState.getCurrentStep, stepEvents, List()))
      }
    })
  }

  private def processMailbox(mbox: Mailbox): Unit = {
    if(mbox.hasMessages) { // DebuggerDispatcher runs this method after enqueuing a message
      ReflectionUtils.callPrivateMethod(mbox, "processAllSystemMessages")()
      ReflectionUtils.callPrivateMethod(mbox, "processMailbox")(1, 0L)
    } else {
      printLog(CmdLineUtils.LOG_WARNING, "Mailbox does not have any messages: " + mbox.messageQueue.numberOfMessages + "   " + mbox.messageQueue.toString)
    }
  }

  private def checkAndWaitForActorBehavior(actor: ActorCell): Unit = {
    if(ReflectionUtils.readPrivateVal(actor, "behaviorStack").asInstanceOf[List[Actor.Receive]] == List.empty) {
      printLog(CmdLineUtils.LOG_DEBUG, "Actor behavior is not set. Cannot process mailbox. Trying again..")
      // We use blocking wait since the ? pattern is run synchronously
      // and the thread dispatching the messages are blocked until this mailbox is processed
      Thread.sleep(500)
      checkAndWaitForActorBehavior(actor)
    }
  }
  private def runOnExecutor(r: Runnable): Unit = {
    executorService execute r
  }

  /**
    * Overriden to intercept and keep the dispatched messages
    *
    * @param receiver   receiver of the intercepted message
    * @param invocation envelope of the intercepted message
    */
  override def dispatch(receiver: ActorCell, invocation: Envelope): Unit = {
    //println("In dispatch : " + invocation + " " + Thread.currentThread().getName)

    invocation match {
      // Handle Dispatcher messages
      case Envelope(msg, _) if msg.isInstanceOf[DispatcherMsg] =>

        msg match {
          // no check for initialization since the actors are not visible to be selected before initiating
          case DispatchToActor(actor) =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleDispatchToActor(actor))
            }))
            return
          case DispatchToNextActor if ExecutionState.isInInitialStep && !ExecutionState.isCurrentStepInHistory =>
            runOnExecutor(toRunnable(() => {
              printLog(CmdLineUtils.LOG_ERROR, "Has not initiated yet.")
              ioProvider.putResponse(
                ActionResponse(noStepNum, List(Log(Events.LOG_ERROR,
                  "Has not initiated yet.", isSuppressed = logLevel > Events.LOG_WARNING)), List())
              )
            }))
            return
          case DispatchToNextActor =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleDispatchToNextActor())
            }))
            return
          case AskActorState(actor, toGet) =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleAskActorState(actor, toGet))
            }))
            return
          case TagActor(actor, toTag) =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleTagActor(actor, toTag))
            }))
            return
          case SuppressActor(actor, toSuppress) =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleSuppressActor(actor, toSuppress))
            }))
            return
          case GotoStep(targetStep) =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleGoToStepInHistory(targetStep))
            }))
            return
          case DropActorMsg(actor) =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleDropActorMsg(actor))
            }))
            return
          case SendResponse(response) =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(response)
            }))
            return
          case InitDispatcher =>
            runOnExecutor(toRunnable(() => {
              ioProvider.putResponse(handleInitiate())
            }))
            return
          case EndDispatcher =>
            runOnExecutor(toRunnable(() => {
              handleTerminate
            }))
            return
        }

      // Do not intercept the log messages
      case Envelope(Error(_, _, _, _), _)
           | Envelope(Warning(_, _, _), _)
           | Envelope(Info(_, _, _), _)
           | Envelope(Debug(_, _, _), _) =>
        printLog(CmdLineUtils.LOG_DEBUG, "Log msg is delivered. Running synchronously. " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        //registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        // Instead of posting the msg handler runnable, synchronously run it (the msg handler just does logging)
        processMailbox(mbox)
        return

      // Do not intercept the messages sent to the system actors
      case _ if DispatcherUtils.isSystemActor(receiver.self) =>
        //CmdLineUtils.printlnForLogging("Not intercepted msg to system actor: " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      // Do not intercept Tcp connection internal messages
      case Envelope(Tcp.Register(_, _, _), _)
           | Envelope(Bound(_), _)
           | Envelope(Connected(_, _), _)
           | Envelope(CommandFailed(_), _)
           | Envelope(Received(_), _) =>
        //CmdLineUtils.printlnForLogging("Not intercepted internal msg To: " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      case _ if Settings.debuggerSettings.noInterceptMsgs.exists(x => invocation.message.toString.startsWith(x)) =>
        printLog(CmdLineUtils.LOG_DEBUG, "Not intercepted: " + receiver.self + " " + invocation)
        val mbox = receiver.mailbox
        mbox.enqueue(receiver.self, invocation)
        registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
        return

      case _ =>
        // if the message is sent by the Ask Pattern:
        if (invocation.sender.isInstanceOf[PromiseActorRef]) {
          printLog(CmdLineUtils.LOG_DEBUG, "-- Message by AskPattern. Sending for execution. " + receiver.self + " " + invocation)
          checkAndWaitForActorBehavior(receiver)
          val mbox = receiver.mailbox
          mbox.enqueue(receiver.self, invocation)
          // registerForExecution posts the msg processing runnable to the thread pool executor
          // (It gets blocked since the only thread in the thread pool is possibly waiting on a future)
          // In case of ?, the msg is synchronously executed in the dispatcher thread

          // what if the handler of this thread waits as well?
          // Sln: Run each on a fresh thread..
          val t = new Thread(toRunnable(() => processMailbox(mbox)))
          t.start()
          t.join(10000)
          return
        } else {
          if (invocation.message.equals(GetInternalActorState)) return // do nth if forwarded dispatcher state msg
        }
      // Go with the default execution, intercept and record the message
    }

    printLog(CmdLineUtils.LOG_INFO, "Intercepting msg to: " + receiver.self + " " + invocation + " " + Thread.currentThread().getName)
    ExecutionState.messageSent(receiver, invocation, suppressedActors.contains(invocation.sender))

    //// Commented out the following original code to block default enqueue to the actor's mailbox
    //// mbox.enqueue(receiver.self, invocation)
    //// registerForExecution(mbox, hasMessageHint = true, hasSystemMessageHint = false)
  }

  /**
    * Overriden to update the actorMap with termination and other system messages
    *
    * @param receiver   receiver of the system message
    * @param invocation the dispatched system message
    */
  override def systemDispatch(receiver: ActorCell, invocation: SystemMessage): Unit = {
    printLog(CmdLineUtils.LOG_DEBUG, "Delivered system msg: " + invocation + "   Actor: " + receiver.self)

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
        printLog(CmdLineUtils.LOG_INFO, "Handling system msg terminates: " + receiver.self)
        ExecutionState.actorDestroyed(receiver, suppressedActors.contains(receiver.self))
        if (watchedActors.contains(receiver)) watchedActors.remove(receiver)
        if (taggedActors.contains(receiver)) taggedActors.remove(receiver)

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
    printLog(CmdLineUtils.LOG_DEBUG, "--------Created mailbox for: " + actor.self.path.toString + " in thread: " + Thread.currentThread().getName)

    ExecutionState.actorCreated(actor, suppressedActors.contains(actor.self)) //todo revise suppressed

    new Mailbox(mailboxType.create(Some(actor.self), Some(actor.system))) with DefaultSystemMessageQueue
  }

  /**
    * Overriden to output the recorded events
    */
  override def shutdown: Unit = {
    printLog(CmdLineUtils.LOG_INFO, "Shutting down.. ")
    super.shutdown
  }

}

class DebuggingDispatcherConfigurator(config: Config, prerequisites: DispatcherPrerequisites)
  extends MessageDispatcherConfigurator(config, prerequisites) {

  private val instance = new DebuggingDispatcher(
    this,
    config.getString("id"),
    Duration(config.getDuration("shutdown-timeout", TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS))

  override def dispatcher(): MessageDispatcher = instance
}
