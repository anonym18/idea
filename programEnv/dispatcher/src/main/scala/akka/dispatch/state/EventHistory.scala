package akka.dispatch.state

import akka.actor.{ActorRef, Cell}
import akka.dispatch.Envelope
import akka.dispatch.state.Messages.MessageId
import debugger.protocol._

import scala.collection.mutable.ListBuffer

/**
  * Each time an actor receives and processes a message, we have a new program step
  * @param receivedMsgId The id of the message that is received (i.e. the events in that step are dependent on this message)
  * @param events (e.g. ActorCreated, MessageSent, Log) generated during the processing of the received message
  * @param states The updated actor states (only the state of the receiving actor except for the initial program step)
  */
case class ProgramStep(receivedMsgId: MessageId, events: List[Event], states: Set[State], messages: Map[Cell, List[Envelope]]) {
  override def toString: String = "\nEvents: " + events + "\nStates: " + states + "\nMessages: " + messages
}

/**
  * Maintains the list of program steps in the execution of the program
  */
class EventHistory {

  /**
    * The list of program steps in the current step
    */
  private val steps: ListBuffer[ProgramStep] = ListBuffer() += ProgramStep(0, List(), Set(), Map())
  // steps(0) keeps the initial state with no actors and messages
  private var currentStep = 1
  private var maxStep = 1
  private var currentMsg = 0

  /**
    * The list of events occurred in the current step
    * Together with the message ids to be used for building the causality relation
    */
  private val buffer: ListBuffer[Event] = ListBuffer()

  /**
    * Adds an event(e.g. MessageSent) to the current program step
    */
  //def addEvent(id: MessageId, e: Event): ListBuffer[(MessageId, Event)] = buffer += ((id, e))

  def addEvent(e: Event): Unit = buffer += e

  def getAllEvents: List[Event] = steps.toList.flatMap(x => x.events)

  /**
    * Adds a new program step consuming the buffer
    * @param states the set of states which has changed due to the receival of the message in the program step
    *               In the initialization: The initial states of all actors
    *               After message receival: The state of only the message receiving actor
    *               After message drop: No state change, empty set
    * @return the list of events consumed (e.g. events in the last step)
    */
  def consumeBufferIntoStep(states: Set[State], messages: Map[Cell, List[Envelope]]): (MessageId, List[Event]) =
    if(buffer.nonEmpty) {
      require(currentStep == 1 || buffer.toList.last.isInstanceOf[Log]
        || buffer.toList.count(e => e.isInstanceOf[MessageReceivedByARef]) == 1
        || buffer.toList.count(e => e.isInstanceOf[MessageDroppedByARef]) == 1)

      val list = buffer.toList
      var messageId: MessageId = Messages.NO_MSG

      list.head match {
        case e: MessageReceivedByARef =>
          messageId = e.msg._1
          steps += ProgramStep(e.msg._1, list, states, messages)
        case e: MessageDroppedByARef =>
          messageId = e.msg._1
          steps += ProgramStep(e.msg._1, list, states, messages)
        case _ =>
          steps += ProgramStep(Messages.NO_MSG, list, states, messages)
      }

      buffer.clear
      currentStep += 1
      maxStep += 1
      (messageId, list)
    } else (Messages.NO_MSG, List())

  def hasReceivedMsgInStep: Boolean = buffer.toList.exists(e => e.isInstanceOf[MessageReceivedByARef] || e.isInstanceOf[MessageDroppedByARef])
  def getBufferContent: List[Event] = buffer.toList

  def getCurrentStep: Int = currentStep
  def isInInitialStep: Boolean = currentStep <= 1 // goto step 1 moves to the step just before receiving the 1st msg

  /**
    * Checks if the currently visualized state is in the history or not
    * If so, the Dispatcher should not allow message receive actions to a selected actor
    *    and can only replay the executed part of the trace
    * @return true if there are executed program steps which are not visualized
    */
  def isCurrentStepInHistory: Boolean = currentStep < maxStep

  /**
    * Moves to a past program step in the event history
    * Called only when a history step is being visualized to the user
    * @return the list of events to be visualized and the actor states at this program step
    */
  def goBackInHistory(targetStep: Int): Option[(List[Event], List[State])] = if (targetStep < currentStep && targetStep >= 0) {
    // events to visualize to go back to the program state in step targetStep
    val eventsToVisualize: ListBuffer[debugger.protocol.Event] = ListBuffer()

    // get all the events between the targetStep and the current step (includes targetStep+1, excludes currentStep+1)
    val eventsInBetween: List[Event] = steps.toList.slice(targetStep+1, currentStep+1).flatMap(step => step.events)

    // add ActorDestroyed events for the actors not existent in stepNum but existent in currentStep
    eventsInBetween.filter(e => e.isInstanceOf[ActorCreatedByARef]).foreach(e => {
      val event = e.asInstanceOf[ActorCreatedByARef]
      eventsToVisualize += ActorDestroyed(event.actor.path.name, event.resourceId, event.isSuppressed)
    })

    // add ActorCreated events for the actors existent in stepNum but not in currentStep
    eventsInBetween.filter(e => e.isInstanceOf[ActorDestroyedByARef]).foreach(e => {
      val event = e.asInstanceOf[ActorDestroyedByARef]
      eventsToVisualize += ActorCreated(event.actor.path.name, event.resourceId, event.isSuppressed)
      })

    // add MessageSent events (all messages of the target step)
    steps(targetStep).messages.foreach(x => x._2.foreach(m =>
      eventsToVisualize += MessageSent(x._1.self.path.name, m.sender.path.name, m.message.toString, isSuppressed = true)))

    currentStep = targetStep + 1
    Some(eventsToVisualize.toList, steps.toList(targetStep).states.toList)

  } else None

  /**
    * Moves to the next program step in the event history
    * Called only when a history step is being visualized to the user
    * @return the list of events to be visualized and the actor states at this program step
    */
  def goFwInHistory: (List[Event], List[State]) = {
    require(currentStep < maxStep) //maxStep-1 is full, maxStep is not
    val result = (steps(currentStep).events.map(EventHistory.convertToIOEvent), steps(currentStep).states.toList)
    currentStep = currentStep + 1
    result
  }

  override def toString: String = steps.indices.zip(steps).map(x => new String("\n" + x._1 + x._2.toString)).toString
}

object EventHistory {
  // todo Adapter?
  /** Translate an event used in the program to the event to be used in the communication */
  def convertToIOEvent(in: Event): Event = in match {
    case event: ActorCreatedByARef => ActorCreated(event.actor.path.name, event.resourceId, event.isSuppressed)
    case event: ActorDestroyedByARef => ActorDestroyed(event.actor.path.name, event.resourceId, event.isSuppressed)
    case event: MessageSentByARef => MessageSent(event.receiver.path.name, event.sender.path.name, event.msg._2.message.toString, event.isSuppressed)
    case event: MessageReceivedByARef => MessageReceived(event.receiver.path.name, event.sender.path.name, event.msg._2.message.toString, event.isSuppressed)
    case event: MessageDroppedByARef => MessageDropped(event.receiver.path.name, event.sender.path.name, event.msg._2.message.toString, event.isSuppressed)
    case _ => in
  }
}


/** Event types used in the programming infrastructure (uses ActorRef instead of String, Envelope instead of String)**/
case class ActorCreatedByARef(actor: ActorRef, resourceId: String, isSuppressed: Boolean) extends Event(isSuppressed)

case class ActorDestroyedByARef(actor: ActorRef, resourceId: String, isSuppressed: Boolean) extends Event(isSuppressed)

case class MessageSentByARef(receiver: ActorRef, sender: ActorRef, msg: (MessageId, Envelope), isSuppressed: Boolean) extends Event(isSuppressed)

case class MessageReceivedByARef(receiver: ActorRef, sender: ActorRef, msg: (MessageId, Envelope), isSuppressed: Boolean) extends Event(isSuppressed)

case class MessageDroppedByARef(receiver: ActorRef, sender: ActorRef, msg: (MessageId, Envelope), isSuppressed: Boolean) extends Event(isSuppressed)