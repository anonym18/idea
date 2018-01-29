import akka.actor.ActorRef

package object app {
  sealed trait Message

  case object Smoke extends Message {
    override def toString: String = "Smoke"
  }
  case object FinishSmoking extends Message {
    override def toString: String = "FinishSmoking"
  }
  case object InvokeAgent extends Message {
    override def toString: String = "InvokeAgent"
  }

  case class Take(smoker: ActorRef) extends Message {
    override def toString: String = "Take(" + smoker.path.name +")"
  }
  case class Taken(item: ActorRef) extends Message {
    override def toString: String = "Taken(" + item.path.name +")"
  }
  case class Busy(item: ActorRef) extends Message {
    override def toString: String = "Busy(" + item.path.name +")"
  }
  case class Put(smoker: ActorRef) extends Message {
    override def toString: String = "Put(" + smoker.path.name +")"
  }
  case class Consumed(smoker: ActorRef) extends Message {
    override def toString: String = "Consumed(" + smoker.path.name +")"
  }
  case class Release(item: ActorRef) extends Message {
    override def toString: String = "Release(" + item.path.name +")"
  }
  case class NotReleased(item: ActorRef) extends Message {
    override def toString: String = "NotReleased"
  }
}
