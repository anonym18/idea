import akka.actor.ActorRef

package object app {

  case class GoToBarberChair(customer: ActorRef) {
    override def toString: String = "GoToBarberChair(" + customer.path.name + ")"
  }
  case class WantsBarberChair(customer: ActorRef) {
    override def toString: String = "WantsBarberChair(" + customer.path.name + ")"
  }
  case class FreesBarberChair(customer: ActorRef) {
    override def toString: String = "FreesBarberChair(" + customer.path.name + ")"
  }
  case object OnBarberChair
  case object BarberChairOccupied

  case class CutHair(customer: ActorRef) {
    override def toString: String = "CutHair(" + customer.path.name + ")"
  }
  case object ProcessingDone
  case object HairCutDone
  case object BarberChairFree

  case object InWaitingRoom
  case class GoesToWaitingRoom(customer: ActorRef) {
    override def toString: String = "GoesToWaitingRoom(" + customer.path.name + ")"
  }
  case object WaitingRoomFull
  case object EntersShop
  case object ExitsShop
}
