package dining.hakkers

//http://www.lightbend.com/activator/template/akka-sample-fsm-scala#code/src/main/scala/sample/become/DiningHakkersOnBecome.scala

import akka.actor.ActorSystem
import akka.dispatch.DebuggingDispatcher

object DiningHakkers extends App {
  val system = ActorSystem("DiningHakkers")

  //Create 5 chopsticks
  val chopsticks = for(i <- 1 to 5) yield system.actorOf(Chopstick.props("chopstick-"+i), "chopstick-"+i)

  //Create 5 awesome hakkers and assign them their left and right chopstick
  val hakkers = for {
    (name,i) <- List("A","B","C","D","E").zipWithIndex
  } yield system.actorOf(Hakker.props("philosopher-" + name, chopsticks(i), chopsticks((i+1) % 5)), "philosopher-" + name)

    //Signal all hakkers that they should start thinking, and watch the show
  hakkers.foreach(_ ! Think)

  DebuggingDispatcher.setActorSystem(system)
  DebuggingDispatcher.setUp()
}
