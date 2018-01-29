package pl.project13.scala.akka.raft.cluster

import org.scalatest.concurrent.PatienceConfiguration
import org.scalatest.time._

trait ClusterPatience extends PatienceConfiguration {
  override implicit val patienceConfig =
    PatienceConfig(
      timeout = scaled(Span(8, Seconds)),
      interval = scaled(Span(500, Millis))
    )

}