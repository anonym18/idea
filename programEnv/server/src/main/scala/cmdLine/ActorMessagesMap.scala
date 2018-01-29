package cmdLine

import scala.collection.{Set, mutable}
import scala.util.control.Breaks.{break, breakable}

class ActorMessagesMap {

  private var actorMessages = mutable.HashMap[String, List[String]]()
  private var taggedActors = mutable.Set[String]()
  private var watchedActors = mutable.Set[String]()
  private var suppressedActors = mutable.Set[String]()

  def addMessage(actor: String, msg: String): Unit = {
    val msgs: List[String] = actorMessages.getOrElse(actor, List[String]())
    actorMessages += (actor -> (msgs :+ msg) )
  }

  def numActors: Int = actorMessages.keySet.size

  def getAllActors: Set[String] = {
    actorMessages.keySet
  }

  def getActorIdByItsRank(no: Int): String = {
    if(numActors < no) {
      throw new Exception("ActorMessagesMap - Actor index requested is out of bounds")
    }
    val sorted = actorMessages.toList.sortBy(_._1)
    sorted(no-1)._1
  }

  def addActor(actor: String): Any = {
    actorMessages.get(actor) match {
      case None => actorMessages += (actor -> List() )
      case Some(msgList) =>   System.err.println("ActorMessagesMap - Message received before actor added into the map")
    }
  }

  def removeActor(actor: String): Any = {
    actorMessages.get(actor) match {
      case Some(msgList) =>   actorMessages.remove(actor)
      // the case for the utility actors not added onto the map
      case None => //System.err.println("ActorMessagesMap - Terminated an actor that does not exist in the map: " + actor.self)
    }
  }

  def clearAllMessages(): Unit = actorMessages.keySet.foreach(actor => actorMessages += (actor -> List()))

  def isTaggedActor(actor: String): Boolean = taggedActors.contains(actor)

  def toggleTagActor(actor: String): Boolean = if(isTaggedActor(actor)) taggedActors.remove(actor) else taggedActors.add(actor)

  def isWatchedActor(actor: String): Boolean = watchedActors.contains(actor)

  def toggleWatchActor(actor: String): Boolean = if(isWatchedActor(actor)) watchedActors.remove(actor) else watchedActors.add(actor)

  def isSuppressedActor(actor: String): Boolean = suppressedActors.contains(actor)

  def toggleSuppressActor(actor: String): Boolean = if(isSuppressedActor(actor)) suppressedActors.remove(actor) else suppressedActors.add(actor)

  def removeHeadMessage(actor: String): Option[String] = {
    actorMessages.get(actor) match {
      case None | Some(Nil) => None
      case Some(msgList) =>
        actorMessages += (actor -> msgList.tail)
        Some(msgList.head)
    }
  }

  //todo does not keep sender, strengthen with the sender check
  def removeMessage(receiver: String, senderId: String, message: String): Unit = {
    actorMessages(receiver).filter(m => m.equals(message))
  }

  /**
    * @return true if there are no actors or the message lists of all actors are empty
    */
  def isAllEmptyExceptLogger: Boolean = {
    def check(keys: Iterable[String]): Boolean = {
      for(k <- keys) {
        breakable {
          if (k.self.toString().contains("log1-Logging$DefaultLogger")) break
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
     def check(keys: Iterable[String]): Boolean = {
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
  def isEmpty(a: String): Boolean = actorMessages.get(a) match {
    case Some(list) => list.isEmpty
    case None => true // must not hit here
  }

  def toMapWithActorRef: mutable.HashMap[String, List[String]] = {
    actorMessages.map(a => (a._1.self, a._2)).clone()
  }

  def printActorMap: Unit = CmdLineUtils.printListOfMap(actorMessages.toList.sortBy(_._1), println)
}
