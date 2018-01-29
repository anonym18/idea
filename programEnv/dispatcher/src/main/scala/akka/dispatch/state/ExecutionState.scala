package akka.dispatch.state

import akka.actor.{ActorRef, Cell}
import akka.dispatch.DebuggingDispatcher.printLog
import akka.dispatch._
import akka.dispatch.state.DependencyGraphBuilder.Dependency
import akka.dispatch.state.Messages._
import akka.dispatch.util.{CmdLineUtils, DispatcherUtils, ReflectionUtils}
import debugger.protocol._


object ExecutionState {
  /**
    * Keeps the messages in the system
    */
  private val messages = new Messages

  /**
    * Keeps the pending messages sent to an actor but not delivered to mailbox yet
    */
  private val actorMessagesMap: ActorMessagesMap = new ActorMessagesMap(messages)

  /**
    * Keeps the dependencies between the message (calculated by hb relation)
    */
  private val dependencyGraphBuilder = new DependencyGraphBuilder()

  /**
    * Keeps the events produced in the program while processing messages
    */
  private val history: EventHistory = new EventHistory()

  /**
    * Keeps the id of the last received message - the events in the current step depend on that message
    */
  private var lastReceived: MessageId = 0
  // add an initial message when dispatcher is initialized
  // before the events generated in the beginning (not in response to the receipt of a message)
  messages.addMessage(ActorRef.noSender, ReflectionUtils.createNewEnvelope("", ActorRef.noSender))
  messages.setProcessed(lastReceived)

  /** State update methods for each program event **/

  /**
    * The actor receives the next message in its buffer
    */
  def receiveMessage(receiver: Cell, isSuppressed: Boolean): Option[Envelope] = {
    actorMessagesMap.removeHeadMessage(receiver) match {
      case Some((id, envelope)) =>
        lastReceived = id
        messages.setProcessed(id)
        history.addEvent(MessageReceivedByARef(receiver.self, envelope.sender, (id, envelope), isSuppressed))
        Some(envelope)
      case None => None
    }
  }

  /**
    * The actor receives the specified message - checks causality
    */
  def receiveMessage(receiver: Cell, senderName: String, message: String, isSuppressed: Boolean): Option[Envelope] = {
    actorMessagesMap.removeMessage(receiver, senderName, message) match {
      case Some((id, envelope)) if isMessageEnabled(id) =>
        lastReceived = id
        messages.setProcessed(id)
        history.addEvent(MessageReceivedByARef(receiver.self, envelope.sender, (id, envelope), isSuppressed))
        Some(envelope)
      case _ => None
    }
  }

  /**
    * The next message in the buffer of the actor is dropped
    */
  def dropMessage(actor: Cell, isSuppressed: Boolean): Option[Envelope] = {
    actorMessagesMap.removeHeadMessage(actor) match {
      case Some((id, envelope)) =>
        lastReceived = id
        messages.setProcessed(id)
        history.addEvent(MessageDroppedByARef(actor.self, envelope.sender, (id, envelope), isSuppressed))
        Some(envelope)
      case None => None
    }
  }

  /**
    * The specified message is dropped - checks causality
    */
  def dropMessage(actor: Cell, senderName: String, message: String, isSuppressed: Boolean): Option[Envelope] = {
    actorMessagesMap.removeMessage(actor, senderName, message) match {
      case Some((id, envelope)) if isMessageEnabled(id) =>
        lastReceived = id
        messages.setProcessed(id)
        history.addEvent(MessageDroppedByARef(actor.self, envelope.sender, (id, envelope), isSuppressed))
        Some(envelope)
      case _ => None
    }
  }

  def messageSent(receiver: Cell, invocation: Envelope, isSuppressed: Boolean) = {

    val messageId = messages.addMessage(receiver.self, invocation)
    actorMessagesMap.addMessage(receiver, messageId)

    // Add the intercepted message into the list of output events
    history.addEvent(MessageSentByARef(receiver.self, invocation.sender, (messageId, invocation), isSuppressed)) // suppress if the sender is suppressed
    // todo add a method to check suppressed
  }

  def actorCreated(actor: Cell, isSuppressed: Boolean) = {
    // add to the event list only if it is not a system actor
    if (!DispatcherUtils.isSystemActor(actor.self)) {
      Settings.actorSettings(actor.self.path.name).visualResourceId match {
        case Some(resource) => history.addEvent(ActorCreatedByARef(actor.self, resource, Settings.actorSettings(actor.self.path.name).isSuppressed)) //todo suppress
        case None =>
          printLog(CmdLineUtils.LOG_DEBUG, "No resource for the actor: " + actor.self.path.name + " provided in configuration file. Using default actor resource")
          history.addEvent(ActorCreatedByARef(actor.self, "", Settings.actorSettings(actor.self.path.name).isSuppressed))
      }

      actorMessagesMap.addActor(actor)
    }
  }

  def actorDestroyed(actor: Cell, isSuppressed: Boolean) = {
    // add to the event list only if it is not a system actor
    if (!DispatcherUtils.isSystemActor(actor.self)) {
      Settings.actorSettings(actor.self.path.name).visualResourceId match {
        case Some(resource) => history.addEvent(ActorDestroyedByARef(actor.self, resource, isSuppressed)) //todo suppress
        case None =>           history.addEvent(ActorDestroyedByARef(actor.self, "", isSuppressed))
      }
      ExecutionState.actorMessagesMap.removeActor(actor)
    }
  }

  def addLogEvent(event: Log) = history.addEvent(event)

  def addInitialEvent = {
    history.addEvent(MessageReceivedByARef(ActorRef.noSender, ActorRef.noSender, (0L, ReflectionUtils.createNewEnvelope("", ActorRef.noSender)), isSuppressed = true))
    messages.addMessage(ActorRef.noSender, ReflectionUtils.createNewEnvelope("", ActorRef.noSender))
  }

  /**
    * Returns whether a message is enabled (i.e. all its predecessors are already executed)
    */
  private def isMessageEnabled(id: MessageId): Boolean = !dependencyGraphBuilder.predecessors(id).map(x => x._2).exists(!messages.isProcessed(_))

  /** Getters  **/
  def getActor(actorName: String): Option[Cell] = actorMessagesMap.getActor(actorName)

  def getAllActors = actorMessagesMap.getAllActors

  def isInInitialStep: Boolean = history.isInInitialStep

  def isCurrentStepInHistory: Boolean = history.isCurrentStepInHistory

  def getCurrentStep: Int = history.getCurrentStep

  def getHistory: String = history.toString

  def getAllEvents: List[Event] = history.getAllEvents

  def getAllMessages: List[Message] = messages.getAllMessages.toList.sortBy(m => m.id)

  def getReceiveDropEvents: List[Event] = getAllEvents.filter(
    e => e.isInstanceOf[MessageReceivedByARef] || e.isInstanceOf[MessageDroppedByARef]).map(EventHistory.convertToIOEvent)

  /** Modifiers  **/

  def goFwInHistory: (List[Event], List[State]) = history.goFwInHistory

  def goBackInHistory(targetStep: Int) = history.goBackInHistory(targetStep)

  def endCurrentStep(updatedStates: Set[State]): List[Event] = {
    // calculate dependencies of the newly created messages in response to the received message
    val (id, events) = history.consumeBufferIntoStep(updatedStates, actorMessagesMap.getActorMessagesMap)
    if(id != Messages.NO_MSG) calculateDependencies(id, events)
    // the events to send to IO
    events.map(EventHistory.convertToIOEvent)
  }

  /**
    * Calculate the dependencies of the messages generated in response to a received message
    * @param list of events generated in response to a message
    * @return set of dependencies (i.e. predecessors in the sense of causality of the messages) of each generated message
    */
  def calculateDependencies(receivedMessageId: MessageId, list: List[Event]): Map[MessageId, Set[MessageId]] = {
    val receivedMessage = messages.get(receivedMessageId) // the first event is always of type MESSAGE_RECEIVED
    val sentMessages = list.filter(_.isInstanceOf[MessageSentByARef]).map(e => messages.get(e.asInstanceOf[MessageSentByARef].msg._1)) // ids of the sent messages
    val createdActors = list.filter(_.isInstanceOf[ActorCreatedByARef]).map(_.asInstanceOf[ActorCreatedByARef].actor) // created actor refs
    dependencyGraphBuilder.calculateDependencies(receivedMessage, sentMessages, createdActors).map(pair => (pair._1, pair._2.map(dep => dep._2)))
  }

  def getPredecessors(messageId: MessageId): Set[MessageId] = dependencyGraphBuilder.predecessors(messageId).map(dep => dep._2)

  def getAllPredecessors: Set[(MessageId, Set[MessageId])] = messages.getAllMessageIds.map(id => (id, dependencyGraphBuilder.predecessors(id)))
    .map(pair => (pair._1, pair._2.map(dep => dep._2)))

  def getAllPredecessorsWithDepType: Set[(MessageId, Set[Dependency])] = messages.getAllMessageIds.map(id => (id, dependencyGraphBuilder.predecessors(id)))
}



