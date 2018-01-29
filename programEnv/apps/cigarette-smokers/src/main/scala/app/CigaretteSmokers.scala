package app

import akka.actor.ActorSystem
import akka.dispatch.DebuggingDispatcher

object CigaretteSmokers extends App {
  val system = ActorSystem("Cigarette-Smokers")

  //Create items
  val tobacco = system.actorOf(Item.props("Tobacco"), "Tobacco")
  val paper = system.actorOf(Item.props("Paper"), "Paper")
  val matches = system.actorOf(Item.props("Matches"), "Matches")
  val items = List(paper, matches, tobacco)

  // create Agent
  val agent = system.actorOf(Agent.props(tobacco, paper, matches), "Agent")

  //Create smokers and assign their items
  val smokers = for {
    (name,i) <- List("Tobacco","Paper","Matches").zipWithIndex
  } yield system.actorOf(Smoker.props(items(i), items((i+1) % 3), agent), "SmokerWith" + name)

  agent ! InvokeAgent
  smokers.foreach(_ ! Smoke)

  DebuggingDispatcher.setActorSystem(system)
  DebuggingDispatcher.setUp()
}
