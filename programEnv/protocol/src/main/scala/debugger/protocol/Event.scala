package debugger.protocol

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._


abstract class Event(isSuppressed: Boolean)

/** Event types used in the IO (communication to the UI and read from/write to file), uses String instead of ActorRef **/
case class ActorCreated(actorId: String, resourceId: String, isSuppressed: Boolean) extends Event(isSuppressed)

case class ActorDestroyed(actorId: String, resourceId: String, isSuppressed: Boolean) extends Event(isSuppressed)

case class MessageSent(receiverId: String, senderId: String, msg: String, isSuppressed: Boolean) extends Event(isSuppressed)

case class MessageReceived(receiverId: String, senderId: String, msg: String, isSuppressed: Boolean) extends Event(isSuppressed)

case class MessageDropped(receiverId: String, senderId: String, msg: String, isSuppressed: Boolean) extends Event(isSuppressed)

case class Log(logType: Int, text: String, isSuppressed: Boolean) extends Event(isSuppressed)

object Events extends SprayJsonSupport with DefaultJsonProtocol {

  val ACTOR_CREATED = "ACTOR_CREATED"
  val ACTOR_DESTROYED = "ACTOR_DESTROYED"
  val MESSAGE_SENT = "MESSAGE_SENT"
  val MESSAGE_RECEIVED = "MESSAGE_RECEIVED"
  val MESSAGE_DROPPED = "MESSAGE_DROPPED"
  val LOG = "LOG"

  val LOG_DEBUG = 0
  val LOG_INFO = 1
  val LOG_WARNING = 2
  val LOG_ERROR = 3

  implicit object EventJsonFormat extends RootJsonFormat[Event] {
    def write(event: Event): JsObject = event match {
      case ActorCreated(actorId, resourceId, isSuppressed) => JsObject(
        "eventType" -> JsString(ACTOR_CREATED),
        "actorId" -> JsString(actorId),
        "resourceId" -> JsString(resourceId),
        "isSuppressed" -> JsBoolean(isSuppressed))
      case ActorDestroyed(actorId, resourceId: String, isSuppressed) => JsObject(
        "eventType" -> JsString(ACTOR_DESTROYED),
        "actorId" -> JsString(actorId),
        "resourceId" -> JsString(resourceId),
        "isSuppressed" -> JsBoolean(isSuppressed))
      case MessageSent(receiverId, senderId, msg, isSuppressed) => JsObject(
        "eventType" -> JsString(MESSAGE_SENT),
        "receiverId" -> JsString(receiverId),
        "senderId" -> JsString(senderId),
        "msg" -> JsString(msg),
        "isSuppressed" -> JsBoolean(isSuppressed))
      case MessageReceived(receiverId, senderId, msg, isSuppressed) => JsObject(
        "eventType" -> JsString(MESSAGE_RECEIVED),
        "receiverId" -> JsString(receiverId),
        "senderId" -> JsString(senderId),
        "msg" -> JsString(msg),
        "isSuppressed" -> JsBoolean(isSuppressed))
      case MessageDropped(receiverId, senderId, msg, isSuppressed) => JsObject(
        "eventType" -> JsString(MESSAGE_DROPPED),
        "receiverId" -> JsString(receiverId),
        "senderId" -> JsString(senderId),
        "msg" -> JsString(msg),
        "isSuppressed" -> JsBoolean(isSuppressed))
      case Log(logType, text, isSuppressed) => JsObject(
        "eventType" -> JsString(LOG),
        "logType" -> JsNumber(logType),
        "text" -> JsString(text),
        "isSuppressed" -> JsBoolean(isSuppressed))
      case _ => serializationError("Event cannot be read")
    }

    def read(json: JsValue): Event = {
      val fields = json.asJsObject.fields
      fields("eventType") match {
        case JsString(Events.ACTOR_CREATED) => ActorCreated(fields("actorId").convertTo[String], fields("resourceId").convertTo[String], fields("isSuppressed").convertTo[Boolean])
        case JsString(Events.ACTOR_DESTROYED) => ActorDestroyed(fields("actorId").convertTo[String], fields("resourceId").convertTo[String], fields("isSuppressed").convertTo[Boolean])
        case JsString(Events.MESSAGE_SENT) => MessageSent(fields("receiverId").convertTo[String], fields("senderId").convertTo[String],
          fields("msg").convertTo[String], fields("isSuppressed").convertTo[Boolean])
        case JsString(Events.MESSAGE_RECEIVED) => MessageReceived(fields("receiverId").convertTo[String], fields("senderId").convertTo[String],
          fields("msg").convertTo[String], fields("isSuppressed").convertTo[Boolean])
        case JsString(Events.MESSAGE_DROPPED) => MessageDropped(fields("receiverId").convertTo[String], fields("senderId").convertTo[String],
          fields("msg").convertTo[String], fields("isSuppressed").convertTo[Boolean])
        case JsString(Events.LOG) => Log(fields("logType").convertTo[Int], fields("text").convertTo[String], fields("isSuppressed").convertTo[Boolean])
        case _ => deserializationError("Event expected")
      }
    }
  }
}
