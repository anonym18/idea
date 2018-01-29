package debugger.protocol

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.{JsString, _}

trait QueryRequest

case class ActionRequest(actionType: String, receiverId: String) extends QueryRequest
case class StateRequest(actorId: String, toGet: Boolean) extends QueryRequest
case class TagRequest(actorId: String, toTag: Boolean) extends QueryRequest
case class SuppressActorRequest(actorId: String, toSuppress: Boolean) extends QueryRequest
case class StepRequest(stepNum: Int) extends QueryRequest
case object TopographyRequest extends QueryRequest

object QueryRequests extends SprayJsonSupport with DefaultJsonProtocol {

  val STATE_REQUEST = "STATE_REQUEST"
  val ACTION_REQUEST = "ACTION_REQUEST"
  val TAGACTOR_REQUEST = "TAGACTOR_REQUEST"
  val SUPPRESS_ACTOR_REQUEST = "SUPPRESS_ACTOR_REQUEST"
  val STEP_REQUEST = "STEP_REQUEST"
  val TOPOGRAPHY_REQUEST = "TOPOGRAPHY_REQUEST"

  val ACTION_INIT = "__INIT__"
  val ACTION_END = "__END__"
  val ACTION_NEXT = "__NEXT__" // next actor if not tagged
  val ACTION_DROP = "__DROP__"

  implicit val receiveRequestFormat: RootJsonFormat[ActionRequest] = jsonFormat2(ActionRequest)
  implicit val stateRequestFormat: RootJsonFormat[StateRequest] = jsonFormat2(StateRequest)
  implicit val suppressRequestFormat: RootJsonFormat[SuppressActorRequest] = jsonFormat2(SuppressActorRequest)
  implicit val stepRequestFormat: RootJsonFormat[StepRequest] = jsonFormat1(StepRequest)
  implicit val tagActorRequestFormat: RootJsonFormat[TagRequest] = jsonFormat2(TagRequest)

  implicit object QueryRequestJsonFormat extends RootJsonFormat[QueryRequest]{

    def write(obj: QueryRequest): JsValue = obj match {
      case StateRequest(actorId, toGet) => JsObject(
        "requestType" -> JsString(STATE_REQUEST),
        "actorId" -> JsString(actorId),
        "toGet" -> JsBoolean(toGet))
      case ActionRequest(actionType, receiverId) => JsObject(
        "requestType" -> JsString(ACTION_REQUEST),
        "actionType" -> JsString(actionType),
        "receiverId" -> JsString(receiverId))
      case TagRequest(actorId, toTag) => JsObject(
        "requestType" -> JsString(TAGACTOR_REQUEST),
        "actorId" -> JsString(actorId),
        "toTag" -> JsBoolean(toTag))
      case SuppressActorRequest(actorId, toSuppress) => JsObject(
        "requestType" -> JsString(SUPPRESS_ACTOR_REQUEST),
        "actorId" -> JsString(actorId),
        "toSuppress" -> JsBoolean(toSuppress))
      case StepRequest(stepNum) => JsObject(
        "requestType" -> JsString(STEP_REQUEST),
        "stepNum" -> JsNumber(stepNum))
      case TopographyRequest => JsObject(
        "requestType" -> JsString(TOPOGRAPHY_REQUEST))
      case _ => serializationError("Query Request cannot be read")
    }

    def read(json: JsValue): QueryRequest =
      json.asJsObject.fields("requestType") match {
        case JsString(QueryRequests.ACTION_REQUEST) => json.convertTo[ActionRequest]
        case JsString(QueryRequests.STATE_REQUEST) => json.convertTo[StateRequest]
        case JsString(QueryRequests.TAGACTOR_REQUEST) => json.convertTo[TagRequest]
        case JsString(QueryRequests.SUPPRESS_ACTOR_REQUEST) => json.convertTo[SuppressActorRequest]
        case JsString(QueryRequests.STEP_REQUEST) => json.convertTo[StepRequest]
        case JsString(QueryRequests.TOPOGRAPHY_REQUEST) => TopographyRequest
        case unknown => deserializationError(s"unknown object: ${unknown}")
      }
  }

}

