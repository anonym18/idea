package dining

import akka.actor.ActorRef

package object hakkers {
  sealed trait DiningHakkerMessage
  case class Busy(chopstick: ActorRef) extends DiningHakkerMessage {
    override def toString = "Busy(" + chopstick.path.name + ")"
  }
  case class Put(hakker: ActorRef) extends DiningHakkerMessage {
    override def toString = "Put(" + hakker.path.name + ")"
  }
  case class Take(hakker: ActorRef) extends DiningHakkerMessage {
    override def toString = "Take(" + hakker.path.name + ")"
  }
  case class Taken(chopstick: ActorRef) extends DiningHakkerMessage {
    override def toString = "Taken(" + chopstick.path.name + ")"
  }
  object Eat extends DiningHakkerMessage {
    override def toString = "Eat"
  }
  object Think extends DiningHakkerMessage {
    override def toString = "Think"
  }
}
