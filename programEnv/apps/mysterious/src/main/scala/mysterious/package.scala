import akka.actor.ActorRef

package object mysterious {
  sealed trait MysteriousMessage
  case class Busy(resource: ActorRef) extends MysteriousMessage {
    override def toString = "Busy(" + resource.path.name + ")"
  }
  case class Release(entity: ActorRef) extends MysteriousMessage {
    override def toString = "Release(" + entity.path.name + ")"
  }
  case class Acquire(entity: ActorRef) extends MysteriousMessage {
    override def toString = "Acquire(" + entity.path.name + ")"
  }
  case class Acquired(resource: ActorRef) extends MysteriousMessage {
    override def toString = "Acquired(" + resource.path.name + ")"
  }
  object WantToProcess extends MysteriousMessage {
    override def toString = "Want To Process"
  }
  object GoIdle extends MysteriousMessage {
    override def toString = "Go Idle"
  }
}
