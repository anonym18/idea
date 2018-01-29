package app.pingpong

import akka.actor.ActorSystem
import akka.actor.Actor
import akka.actor.Props
import akka.testkit.{ TestActors, TestKit, ImplicitSender }
import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import org.scalatest.BeforeAndAfterAll

/**
  * Tests fail due to the time out while using DebuggingDispatcher
  * (which delivers messages depending on the user input)
  * @param _system
  */
class PingPongActorSpec(_system: ActorSystem) extends TestKit(_system) with ImplicitSender
  with WordSpecLike with Matchers with BeforeAndAfterAll {
 
  def this() = this(ActorSystem("MySpec"))
 
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  val pongActor = system.actorOf(PongActor.props)

  "A Ping actor" must {
    "send back a ping on a pong" in {
      val pingActor = system.actorOf(Props(new PingActor(pongActor, 1, true)))
      pingActor ! PongActor.PongMessage("pong")
      expectMsg(PingActor.PingMessage("ping 1"))
    }
  }

  "A Pong actor" must {
    "send back a pong on a ping" in {
      pongActor ! PingActor.PingMessage("ping 1")
      expectMsg(PongActor.PongMessage("pong 1"))
    }
  }

}
