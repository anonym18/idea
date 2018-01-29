package akka.dispatch.state

import akka.actor.{ActorRef, Cell}
import akka.dispatch.Envelope
import akka.dispatch.state.Messages._

import scala.collection.{Set, mutable}
import scala.util.control.Breaks.breakable

class ActorMessagesMap(messages: Messages) {
  private var actorPaths: Map[String, ActorRef] = Map()

  private var actorMessages = mutable.HashMap[Cell, List[MessageId]]()

  def addMessage(actor: Cell, msg: MessageId): Unit = {
    val msgs: List[MessageId] = actorMessages.getOrElse(actor, List[MessageId]())
    actorMessages += (actor -> (msgs :+ msg))
  }

  def getMessage(receiverName: String, senderName: String, message: String): Option[Envelope] = getActor(receiverName) match {
    case Some(receiver) => getMessage(receiver, senderName, message)
    case None => throw new Exception("ActorMessagesMap - No such receiver actor")
  }

  def getMessage(receiver: Cell, senderName: String, message: String): Option[Envelope] = {
    def getMessage(list: List[MessageId]): Option[Envelope] = list match { //todo
      case x :: xs if messages.get(x).envelope.sender.path.name.equals(senderName) && messages.get(x).envelope.toString.equals(message) => Some(messages.get(x).envelope)
      case x :: xs => getMessage(xs)
      case Nil => None
    }
    getMessage(actorMessages(receiver))
  }

  def getActor(actorName: String): Option[Cell] = {
    def helper(actors: List[Cell]): Option[Cell] = {
      actors match {
        case Nil => None
        case x :: xs if x.self.path.name.toString.equals(actorName) => Some(x)
        case x :: xs => helper(xs)
      }
    }
    helper(actorMessages.keySet.toList)
  }

  def getAllActors: Set[Cell] = {
    actorMessages.keySet
  }

  def addActor(actor: Cell): Unit = {
    actorMessages.get(actor) match {
      case None =>
        actorMessages += (actor -> List() )
        actorPaths += (actor.self.path.name -> actor.self)
      case Some(msgList) =>
        System.err.println("ActorMessagesMap - Message received before actor added into the map")
    }
  }

  def removeActor(actor: Cell): Unit = {
    actorMessages.get(actor) match {
      case Some(msgList) => actorMessages.remove(actor)
      // the case for the utility actors not added onto the map
      case None => //System.err.println("ActorMessagesMap - Terminated an actor that does not exist in the map: " + actor.self)
    }
  }

  def removeHeadMessage(actor: Cell): Option[(MessageId, Envelope)] = {
    actorMessages.get(actor) match {
      case None | Some(Nil) => None
      case Some(msgList) =>
        actorMessages += (actor -> msgList.tail)
        Some(msgList.head, messages.get(msgList.head).envelope)
    }
  }

  def removeMessage(receiverId: String, senderId: String, message: String): Option[(MessageId, Envelope)] = getActor(receiverId) match {
    case Some(receiver) => removeMessage(receiver, senderId, message)
    case None => throw new Exception("ActorMessagesMap - No such receiver actor")
  }

  def removeMessage(receiver: Cell, senderId: String, message: String): Option[(MessageId, Envelope)] = {
    def removeMessage(list: List[MessageId], acc: List[MessageId]): (Option[MessageId], List[MessageId]) = list match {
      case x :: xs if messages.get(x).envelope.sender.path.name.equals(senderId) && messages.get(x).envelope.message.toString.equals(message) => (Some(x), acc ++ xs)
      case x :: xs => removeMessage(xs, acc :+ x)
      case Nil => (None, acc)
    }

    val (removedId, newList) = removeMessage(actorMessages(receiver), Nil)
    actorMessages += (receiver -> newList)
    removedId match {
      case Some(id) => Some(id, messages.get(id).envelope)
      case None => None
    }
  }

  def getActorPath(actorName: String): String = actorPaths.get(actorName) match {
    case Some(actorRef) => actorRef.path.toString
    case None => ""
  }

  def getActorName(actor: ActorRef): String = actor.path.name

  /**
    * @return immutable clone of the actorMessages map
    */
  def getActorMessagesMap: Map[Cell, List[Envelope]] = actorMessages.toMap.map(x => x._1 -> x._2.map(messages.get(_).envelope)) // todo revise

  /**
    * @return true if there are no actors or the message lists of all actors are empty
    */
  def isAllEmptyExceptLogger: Boolean = {
    def check(keys: Iterable[Cell]): Boolean = {
      for(k <- keys) {
        breakable {
          if (!isEmpty(k)) return false
        }
      }
      true
    }

    check(actorMessages.keys)
  }

  /**
    * @return true if there are no actors or the message lists of all actors are empty
    */
  def isAllEmpty: Boolean = {
    def check(keys: Iterable[Cell]): Boolean = {
      for(k <- keys) {
        if (!isEmpty(k)) return false
      }
      true
    }

    check(actorMessages.keys)
  }

  /**
    * @return true if the given actor has an empty message list
    */
  def isEmpty(a: Cell): Boolean = actorMessages.get(a) match {
    case Some(list) => list.isEmpty
    case None => true // must not hit here
  }

  def toMapWithActorRef: mutable.HashMap[ActorRef, List[MessageId]] = {
    actorMessages.map(a => (a._1.self, a._2)).clone()
  }

  def toListWithActorRef: List[(ActorRef, List[Envelope])] = {
    var list: List[(ActorRef, List[Envelope])] = List()
    actorMessages.foreach(a => list = list :+ (a._1.self, a._2.map(messages.get(_).envelope))) //todo
    list.sortBy(_._1.toString()) // costly but easier to manage user input
  }
}