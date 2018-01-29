package app.toy

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import akka.pattern.ask
import akka.dispatch.DebuggingDispatcher
import akka.util.Timeout
import app.toy.Action.Execute
import app.toy.Writer.Init

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ToyMain extends App {
  val system = ActorSystem("sys")
  implicit val timeout = Timeout(5000, TimeUnit.MILLISECONDS)

  val writer = system.actorOf(Writer.props, "writer")
  val terminator = system.actorOf(Terminator.props(actionNum = 1, writer), "terminator")
  val action = system.actorOf(Action.props("data", terminator, writer), "action")

  val initialized = writer ? Init
  Await.result(initialized, Duration.Inf)

  action ! Execute

  DebuggingDispatcher.setActorSystem(system)
  DebuggingDispatcher.setUp()
  system.awaitTermination()
}


