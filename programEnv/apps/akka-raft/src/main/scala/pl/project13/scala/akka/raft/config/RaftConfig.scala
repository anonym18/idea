package pl.project13.scala.akka.raft.config

import com.typesafe.config.Config
import java.util.concurrent.TimeUnit
import concurrent.duration._
import akka.actor.Extension

class RaftConfig (config: Config) extends Extension {

  val raftConfig = config.getConfig("akka.raft")

  val defaultAppendEntriesBatchSize = raftConfig.getInt("default-append-entries-batch-size")

  val publishTestingEvents = raftConfig.getBoolean("publish-testing-events")

  val electionTimeoutMin = Duration(raftConfig.getDuration("election-timeout.min", TimeUnit.MILLISECONDS), MILLISECONDS) //.millis 
  val electionTimeoutMax = Duration(raftConfig.getDuration("election-timeout.max", TimeUnit.MILLISECONDS), MILLISECONDS) //.millis

  val heartbeatInterval = Duration(raftConfig.getDuration("heartbeat-interval", TimeUnit.MILLISECONDS), MILLISECONDS) //.millis 

  val clusterAutoDiscoveryIdentifyTimeout = Duration(raftConfig.getDuration("cluster.auto-discovery.identify-timeout", TimeUnit.MILLISECONDS), MILLISECONDS) //.millis //todo
  val clusterAutoDiscoveryRetryCount = raftConfig.getInt("cluster.auto-discovery.retry-count")
}
