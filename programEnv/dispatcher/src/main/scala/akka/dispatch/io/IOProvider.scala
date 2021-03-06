package akka.dispatch.io

import akka.actor.ActorSystem
import debugger.protocol._

trait IOProvider {
  def setUp(system: ActorSystem)
  def putResponse(response: QueryResponse)
}

object NopIOProvider extends IOProvider {
  override def setUp(system: ActorSystem): Unit = {}

  override def putResponse(response: QueryResponse): Unit = {}
}

