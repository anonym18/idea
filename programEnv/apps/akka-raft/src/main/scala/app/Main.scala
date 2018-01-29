package app

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.DebuggingDispatcher
import pl.project13.scala.akka.raft.ClusterConfiguration
import pl.project13.scala.akka.raft.example.WordConcatRaftActor
import pl.project13.scala.akka.raft.protocol._
import pl.project13.scala.akka.raft.example.protocol._

import akka.dispatch.DebuggingDispatcher.GetInternalActorState
import debugger.protocol.{State, StateColor}

trait EventStreamAllMessages {
  this: Actor =>

  override def aroundReceive(receive: Actor.Receive, msg: Any) = {
    context.system.eventStream.publish(msg.asInstanceOf[AnyRef])

    receive.applyOrElse(msg, unhandled)
  }
}

object Main extends App {


  val initialMembers = 5
  val system = ActorSystem("sys")

  protected var _members: Vector[ActorRef] = Vector.empty

  def beforeAll = {
    (1 to initialMembers).toList foreach { i => createActor(s"raft-member-$i") }

    val raftConfiguration = ClusterConfiguration(_members)
    _members foreach { _ ! ChangeConfiguration(raftConfiguration) }
  }

  def createActor(name: String) = {
    val actor = system.actorOf(Props(new WordConcatRaftActor), name)
    _members :+= actor
    actor
  }

  def subscribeElectedLeader(actorRef: ActorRef): Unit =
    system.eventStream.subscribe(actorRef, ElectedAsLeader.getClass)

  def main = {
    beforeAll

    // given
    _members foreach { subscribeElectedLeader(_) }

    val dummy = system.actorOf(Props(new Actor {
      override def receive: Receive = {
        case clientMsg: ClientMessage[Cmnd] => _members(0) ! clientMsg
        case GetInternalActorState => sender() ! State(self.path.name, StateColor(0, 0, 1, 1), "No state")
      }
    }), "client")

    dummy ! ClientMessage(_members(0), AppendWord("BUGGY"))
    dummy ! ClientMessage(_members(1), AppendWord("SAMPLE"))

    DebuggingDispatcher.setActorSystem(system)
    DebuggingDispatcher.setUp()
    Thread.sleep(5000)
    //system.shutdown()
    //system.awaitTermination()
  }

  main
}