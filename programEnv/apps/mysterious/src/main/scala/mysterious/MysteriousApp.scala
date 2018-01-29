package mysterious

import akka.actor.ActorSystem
import akka.dispatch.DebuggingDispatcher

object MysteriousApp extends App {
  val system = ActorSystem("Mysterious")

  val resources = for(i <- 1 to 5) yield system.actorOf(Resource.props("Resource-"+i), "Resource-"+i)

  val entities = for {
    (name,i) <- List("A","B","C","D","E").zipWithIndex
  } yield system.actorOf(Entity.props("Entity-" + name, resources(i), resources((i+1) % 5)), "Entity-" + name)

  entities.foreach(_ ! GoIdle)

  DebuggingDispatcher.setActorSystem(system)
  DebuggingDispatcher.setUp()
}
