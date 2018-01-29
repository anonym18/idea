package akka.dispatch

import akka.dispatch.util.CmdLineUtils
import debugger.protocol._

/**
  * Handles a QueryRequest by calling a particular method of the LoggingDispatcher
  * The called methods of the LoggingDispatcher posts the required job on the dispatcher thread
  *  (executed async on the dispatcher thread, not on the caller thread)
  */
object QueryRequestHandler {
  def handleRequest(request: QueryRequest): Unit = request match {

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_INIT) => // Start Request
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Sending initial list of events..")
      DebuggingDispatcher.initiateDispatcher()

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_END) => // Terminate Request
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Requested to terminate: ")
      DebuggingDispatcher.terminateDispatcher()

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_DROP) => // Next Actor in the Replayed Trace
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Requested next actor: ")
      DebuggingDispatcher.dropActorMsg(receiverId)

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_NEXT) && receiverId.equals("") => // Next Actor in the Replayed Trace
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Selected next actor: " + receiverId)
      DebuggingDispatcher.dispatchToNextActor()

    case ActionRequest(actionType, receiverId) if actionType.equalsIgnoreCase(QueryRequests.ACTION_NEXT) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Selected next actor: " + receiverId)
      DebuggingDispatcher.dispatchToActor(receiverId)

    case StateRequest(actorId, toGet) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Requested state of: " + actorId)
      DebuggingDispatcher.queryActorState(actorId, toGet)

    case SuppressActorRequest(actorId, toSuppress) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Requested suppressing: " + actorId)
      DebuggingDispatcher.suppressActor(actorId, toSuppress)

    case StepRequest(stepNum) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Requested step: " + stepNum)
      DebuggingDispatcher.gotoStep(stepNum)

    case TagRequest(actorId, toTag) =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Requested to tag: " + actorId)
      DebuggingDispatcher.tagActor(actorId, toTag)

    case TopographyRequest =>
      CmdLineUtils.printLog(CmdLineUtils.LOG_INFO, "===== Requested topography.")
      DebuggingDispatcher.sendTopography()

    case _ => System.err.println("Unidentified request")
  }
}