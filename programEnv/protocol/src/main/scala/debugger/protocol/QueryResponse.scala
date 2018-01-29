package debugger.protocol

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import debugger.protocol.Events.EventJsonFormat
import spray.json._

sealed abstract class QueryResponse
case class ActionResponse(stepNum: Int, events: List[Event], states: List[State]) extends QueryResponse
case class TagResponse(actorId: String, toTag: Boolean) extends QueryResponse
case class TagReachedResponse(actorId: String) extends QueryResponse // tagged actor id to receive next
case object EOTResponse extends QueryResponse
case class SuppressActorResponse(actorId: String, toSuppress: Boolean) extends QueryResponse
case class StepResponse(stepNum: Int, events: List[Event], states: List[State]) extends QueryResponse
case class TopographyResponse(topographyType: String, orderedActorIds: List[String]) extends QueryResponse

case class State(actorId: String, behavior: StateColor, vars: String)
case class StateColor(r: Float, g: Float, b: Float, a: Float)

object QueryResponses extends SprayJsonSupport with DefaultJsonProtocol {

  val ACTION_RESPONSE = "ACTION_RESPONSE"
  val TAG_RESPONSE = "TAG_RESPONSE"
  val TAG_REACHED_RESPONSE = "TAG_REACHED_RESPONSE"
  val EOT_RESPONSE = "EOT_RESPONSE"
  val SUPPRESS_ACTOR_RESPONSE = "SUPPRESS_ACTOR_RESPONSE"
  val STEP_RESPONSE = "STEP_RESPONSE"
  val TOPOGRAPHY_RESPONSE = "TOPOGRAPHY_RESPONSE"

  val RING_TOPOGRAPHY = "RING"

  implicit val StateColorFormat: RootJsonFormat[StateColor] = jsonFormat4(StateColor)
  implicit val StateFormat: RootJsonFormat[State] = jsonFormat3(State)

  /**
    * Full RootJsonFormat is written
    */
  implicit object QueryResponseJsonFormat extends RootJsonFormat[QueryResponse] {

    def write(c: QueryResponse): JsObject = c match {
      case ActionResponse(stepNum, events, states) => JsObject(
        "responseType" -> JsString(ACTION_RESPONSE),
        "stepNum" -> JsNumber(stepNum),
        // Events are written as Strings insteadof Jsons for interoperability purposes
        "events" -> JsArray(events.map(_.toJson.toString.toJson).toVector),
        "states" -> JsArray(states.map(_.toJson).toVector))
      case TagResponse(actorId, toTag) => JsObject(
        "responseType" -> JsString(TAG_RESPONSE),
        "actorId" -> JsString(actorId),
        "toTag" -> JsBoolean(toTag))
      case TagReachedResponse(actorId) => JsObject(
        "responseType" -> JsString(TAG_REACHED_RESPONSE),
        "actorId" -> JsString(actorId))
      case EOTResponse => JsObject(
        "responseType" -> JsString(EOT_RESPONSE))
      case SuppressActorResponse(actorId, toSuppress) => JsObject(
        "responseType" -> JsString(SUPPRESS_ACTOR_RESPONSE),
        "actorId" -> JsString(actorId),
        "toSuppress" -> JsBoolean(toSuppress))
      case StepResponse(stepNum, events, states) => JsObject(
        "responseType" -> JsString(STEP_RESPONSE),
        "stepNum" -> JsNumber(stepNum),
        // Events are written as Strings insteadof Jsons for interoperability purposes
        "events" -> JsArray(events.map(_.toJson.toString.toJson).toVector),
        "states" -> JsArray(states.map(_.toJson).toVector))
      case TopographyResponse(topographyType, orderedActorIds) => JsObject(
        "responseType" -> JsString(TOPOGRAPHY_RESPONSE),
        "topographyType" -> JsString(topographyType),
        "orderedActorIds" -> JsArray(orderedActorIds.map(_.toJson).toVector))
      case _ => serializationError("Query Response cannot be read")
    }

    def read(json: JsValue): QueryResponse = {
      val fields = json.asJsObject.fields
      fields("responseType") match {
        // Events are read as Strings insteadof Jsons for interoperability purposes
        case JsString(QueryResponses.ACTION_RESPONSE) => ActionResponse(
          fields("stepNum").asInstanceOf[JsNumber].value.toInt,
          fields("events").asInstanceOf[JsArray].elements.map(str => EventJsonFormat.read(StringJsonFormat.read(str).asJson)).toList,
          fields("states").asInstanceOf[JsArray].elements.map(StateFormat.read).toList)
        case JsString(QueryResponses.TAG_RESPONSE) => TagResponse(
          fields("actorId").asInstanceOf[JsString].value,
          fields("toTag").asInstanceOf[JsBoolean].value)
        case JsString(QueryResponses.TAG_REACHED_RESPONSE) => TagReachedResponse(
          fields("actorId").asInstanceOf[JsString].value)
        case JsString(QueryResponses.EOT_RESPONSE) => EOTResponse
        case JsString(QueryResponses.SUPPRESS_ACTOR_RESPONSE) => SuppressActorResponse(
          fields("actorId").asInstanceOf[JsString].value,
          fields("toSuppress").asInstanceOf[JsBoolean].value)
        case JsString(QueryResponses.STEP_RESPONSE) => StepResponse(
          fields("stepNum").asInstanceOf[JsNumber].value.toInt,
          fields("events").asInstanceOf[JsArray].elements.map(str => EventJsonFormat.read(StringJsonFormat.read(str).asJson)).toList,
          fields("states").asInstanceOf[JsArray].elements.map(StateFormat.read).toList)
        case JsString(QueryResponses.TOPOGRAPHY_RESPONSE) => TopographyResponse(
          fields("topographyType").asInstanceOf[JsString].value,
          fields("orderedActorIds").asInstanceOf[JsArray].elements.map(_.asInstanceOf[JsString].value).toList)
        case _ => deserializationError("Query Response expected")
      }
    }
  }
}


