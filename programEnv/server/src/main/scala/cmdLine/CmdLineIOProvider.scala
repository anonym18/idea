package cmdLine


import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import CmdLineProcessorActor.GetInput
import debugger.protocol._

object CmdLineIOProvider {
  var cmdLineProcessor: Option[ActorRef] = None
  var uiServer: Option[ActorRef] = None

  def initiate(system: ActorSystem, uiRef: Option[ActorRef]): Unit = {
    uiServer = uiRef
    cmdLineProcessor = Some(system.actorOf(CmdLineProcessorActor.props))
    cmdLineProcessor.get ! GetInput
  }

  def putResponse(response: QueryResponse): Unit = {
    cmdLineProcessor match {
      case Some(actor) => actor ! response
      case None => println("The actor for processing command line IO is not created.")
    }
  }
}

// Receives user inputs and displays the received responses
class CmdLineProcessorActor extends Actor {

  val actorMessagesMap = new ActorMessagesMap()
  var tagReached: Boolean = false

  override def receive: Receive = {
    // blocking wait for an input
    case GetInput =>
      actorMessagesMap.printActorMap
      CmdLineUtils.printlnForUiInput("Please enter the next command: " + "\"start\" OR \"quit\" OR \n" +
        "  \"next <index>\" to dispatch the next message of an actor, 0 for the actor in replay trace OR \n" +
        "  \"state <index>\" to display/hide the state of an actor OR \n" +
        "  \"tag <index>\" to tag/untag an actor as a breakpoint OR \n" +
        "  \"suppress <index>\" to suppress/unsuppress an actor (for visualization) OR \n" +
        //"  \"drop <index>\" to drop the next message of an actor OR \n" +
        "  \"goto <index>\" to go back to a step in history")

      val choice = CmdLineUtils.parseInput(Range(0, actorMessagesMap.numActors), List("start", "state", "unstate", "next", "drop", "tag", "suppress", "goto", "quit", "topography"))

      choice match {
        case ("start", _) =>
          sendRequest(ActionRequest(QueryRequests.ACTION_INIT, "")) // interpreted as Start
        case ("next", Some(0)) =>
          println("Requested next actor")
          sendRequest(ActionRequest(QueryRequests.ACTION_NEXT, ""))
        case ("next", Some(actorNo)) =>
          sendRequest(ActionRequest(QueryRequests.ACTION_NEXT, actorMessagesMap.getActorIdByItsRank(actorNo)))
        case ("tag", Some(actorNo)) =>
          println("Tagged actor")
          val actor = actorMessagesMap.getActorIdByItsRank(actorNo)
          actorMessagesMap.toggleTagActor(actor)
          sendRequest(TagRequest(actor, actorMessagesMap.isTaggedActor(actor)))
        case ("drop", Some(actorNo)) =>
          sendRequest(ActionRequest(QueryRequests.ACTION_DROP, actorMessagesMap.getActorIdByItsRank(actorNo)))
        case ("state", Some(actorNo)) =>
          val actor = actorMessagesMap.getActorIdByItsRank(actorNo)
          actorMessagesMap.toggleWatchActor(actor)
          sendRequest(StateRequest("" + actorMessagesMap.getActorIdByItsRank(actorNo),  actorMessagesMap.isWatchedActor(actor)))
        case ("suppress", Some(actorNo)) =>
          val actor = actorMessagesMap.getActorIdByItsRank(actorNo)
          actorMessagesMap.toggleSuppressActor(actor)
          sendRequest(SuppressActorRequest("" + actorMessagesMap.getActorIdByItsRank(actorNo),  actorMessagesMap.isSuppressedActor(actor)))
        case ("goto", Some(stepNum)) =>
          sendRequest(StepRequest(stepNum))
        case ("topography", _) =>
          println("Requested topography")
          sendRequest(TopographyRequest)
        case ("quit", _) =>
          println("Requested to quit")
          sendRequest(ActionRequest(QueryRequests.ACTION_END, ""))
        case _ =>
          CmdLineUtils.printlnForUiInput("Wrong input. Try again.")
          self ! GetInput
      }

    case response: QueryResponse =>
      println("Received response: " + response)
      processResponse(response)
      self ! GetInput // get next user input once the response is received

    case _ => println("Undefined message sent to the CmdLineProcessorActor")
  }

  def sendRequest(request: QueryRequest): Unit = {
    CmdLineIOProvider.uiServer.foreach(_ ! request)
  }

  def processResponse(response: QueryResponse): Unit = response match {
    case ActionResponse(stepNum, events, states) =>
      CmdLineUtils.printlnForUiOutput("StepNum: " + stepNum)
      CmdLineUtils.printlnForUiOutput("Events: " + events)
      CmdLineUtils.printlnForUiOutput("States: " + states)
      updateActorMessages(events)

    case TagResponse(actorId, toTag) =>
      CmdLineUtils.printlnForUiOutput("Actor tag response - Actor: " + actorId + "  toTag: " + toTag)

    case TagReachedResponse(actorId) =>
      CmdLineUtils.printlnForUiOutput("Tagged actor reached: " + actorId)
      tagReached = true

    case EOTResponse =>
      CmdLineUtils.printlnForUiOutput("End of trace is reached.")

    case SuppressActorResponse(actorId, toSuppress) =>
      CmdLineUtils.printlnForUiOutput("Actor suppress response - Actor: " + actorId + "  toSuppress: " + toSuppress)

    case StepResponse(stepNum, events, states) =>
      CmdLineUtils.printlnForUiOutput("Step response: \nStepNum: " + stepNum + "\nEvents: " + events + "\nStates: " + states)
      actorMessagesMap.clearAllMessages()
      updateActorMessages(events)

    case TopographyResponse(topographyType, orderedActorIds) =>
      CmdLineUtils.printlnForUiOutput("Topography Type: " + topographyType)
      CmdLineUtils.printlnForUiOutput("Ordered ActorIds: " + orderedActorIds)

    case _ => // do nth
  }

  private def updateActorMessages(events: List[Event]) = {
    events.foreach {
      case ActorCreated(actorId, _, _) => actorMessagesMap.addActor(actorId)
      case MessageSent(receiverId, senderId, msg, _) => actorMessagesMap.addMessage(receiverId, msg)
      case MessageReceived(receiverId, senderId, msg, _) => actorMessagesMap.removeHeadMessage(receiverId)
      case MessageDropped(receiverId, senderId, msg, _) => actorMessagesMap.removeHeadMessage(receiverId)
      case ActorDestroyed(actorId, _, _) => actorMessagesMap.removeActor(actorId)
      case _ => // do nth
    }
  }
}

object CmdLineProcessorActor {
  def props: Props = Props(new CmdLineProcessorActor()).withDispatcher("akka.actor.pinned-dispatcher")
  case object GetInput
}
