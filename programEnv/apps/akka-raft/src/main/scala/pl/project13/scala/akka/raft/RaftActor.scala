package pl.project13.scala.akka.raft

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, LoggingFSM}
import akka.dispatch.time.MockTime
import debugger.protocol.{State, StateColor}

import scala.concurrent.duration._
import model._
import protocol._

import scala.concurrent.forkjoin.ThreadLocalRandom
import pl.project13.scala.akka.raft.compaction.LogCompactionExtension
import pl.project13.scala.akka.raft.config.RaftConfiguration

abstract class RaftActor extends Actor with LoggingFSM[RaftState, Metadata]
  with ReplicatedStateMachine
  with Follower with Candidate with Leader with SharedBehaviors {

  type Command

  protected val raftConfig = RaftConfiguration(context.system)

  protected val logCompaction = LogCompactionExtension(context.system)

  private val ElectionTimeoutTimerName = "election-timer" + self.path.name //B added path name to make it unique among actors

  /**
   * Used in order to avoid starting elections when we process an timeout event (from the election-timer),
   * which is immediatly followed by a message from the Leader.
   * This is because timeout handling is just a plain old message (no priority implemented there).
   *
   * This means our election reaction to timeouts is relaxed a bit - yes, but this should be a good thing.
   */
  //var electionDeadline: Deadline = 0.seconds.fromNow
  var timeToElection: FiniteDuration = 0.seconds


  // raft member state ---------------------

  var replicatedLog = ReplicatedLog.empty[Command](raftConfig.defaultAppendEntriesBatchSize)

  //B index of the next log entry that will be sent to that server
  //B will replicate the logs after replicatedLog.lastIndex
  var nextIndex = LogIndexMap.initialize(Set.empty, replicatedLog.lastIndex)

  //B for each server, index of highest log entry known to be replicated on server
  var matchIndex = LogIndexMap.initialize(Set.empty, -1)

  // end of raft member state --------------

  val heartbeatInterval: FiniteDuration = raftConfig.heartbeatInterval

  def nextElectionDeadline(): FiniteDuration =
    if(this.self.path.name.endsWith("-1"))
      FiniteDuration(15, TimeUnit.MILLISECONDS)
    else
      randomElectionTimeout(
        from = raftConfig.electionTimeoutMin,
        to = raftConfig.electionTimeoutMax) / 50 * 50 + MockTime.time.elapsed //.fromNow

  override def preStart() {
    log.info("Starting new Raft member, will wait for raft cluster configuration...")
  }

  startWith(Init, Meta.initial)

  when(Init)(initialConfigurationBehavior)

  when(Follower)(followerBehavior orElse snapshottingBehavior orElse clusterManagementBehavior)

  when(Candidate)(candidateBehavior orElse snapshottingBehavior orElse clusterManagementBehavior)

  when(Leader)(leaderBehavior orElse snapshottingBehavior orElse clusterManagementBehavior)

  onTransition {
    case Init -> Follower if stateData.clusterSelf != self =>
      log.info("Cluster self != self => Running clustered via a proxy.")
      resetElectionDeadline()

    case Follower -> Candidate =>
      self ! BeginElection
      resetElectionDeadline()

    case Candidate -> Leader =>
      self ! ElectedAsLeader
      cancelElectionDeadline()

    case _ -> Follower =>
      resetElectionDeadline()
  }

  onTermination {
    case stop =>
      stopHeartbeat()
  }

  initialize() // akka internals; MUST be last call in constructor

  // helpers -----------------------------------------------------------------------------------------------------------

  def cancelElectionDeadline() {
    cancelTimer(ElectionTimeoutTimerName)
  }

  def resetElectionDeadline(): Duration = {
    //cancelTimer(ElectionTimeoutTimerName)
    MockTime.cancelTimer(self, ElectionTimeoutTimerName)

    timeToElection = nextElectionDeadline()
    log.debug("Resetting election timeout: {}", timeToElection)
    log.info("Resetting election timeout: {}", timeToElection)

    // deliver msg after given timeout
    //setTimer(ElectionTimeoutTimerName, ElectionTimeout, electionDeadline.timeLeft, repeat = false) //todo
    //MockTime.time.scheduler.scheduleOnce(timeToElection, new Runnable() {
    //  override def run(): Unit = self ! ElectionTimeout
    //})
    MockTime.setTimer(self, ElectionTimeoutTimerName, ElectionTimeout, timeToElection, repeat = false)

    timeToElection //B the return value is not used in the project
  }

  private def randomElectionTimeout(from: FiniteDuration, to: FiniteDuration): FiniteDuration = {
    val fromMs = from.toMillis
    val toMs = to.toMillis
    require(toMs > fromMs, s"to ($to) must be greater than from ($from) in order to create valid election timeout.")

   //(fromMs + ThreadLocalRandom.current().nextInt(toMs.toInt - fromMs.toInt)).millis //todo revise
    Duration(fromMs + ThreadLocalRandom.current().nextInt(toMs.toInt - fromMs.toInt), MILLISECONDS)
  }

  // named state changes --------------------------------------------

  /** Start a new election */
  def beginElection(m: Meta) = {
    resetElectionDeadline()

    if (m.config.members.isEmpty) {
      // cluster is still discovering nodes, keep waiting
      goto(Follower) using m
    } else {
      goto(Candidate) using m.forNewElection //forMax nextElectionDeadline().timeLeft //todo
    }
  }

  /** Stop being the Leader */
  def stepDown(m: LeaderMeta) = {
    goto(Follower) using m.forFollower
  }

  /** Stay in current state and reset the election timeout */
  def acceptHeartbeat() = {
    resetElectionDeadline()
    stay()
  }

  // end of named state changes -------------------------------------

  /** `true` if this follower is at `Term(2)`, yet the incoming term is `t > Term(3)` */
  def isInconsistentTerm(currentTerm: Term, term: Term): Boolean = term < currentTerm

  // sender aliases, for readability
  @inline def follower() = sender()
  @inline def candidate() = sender()
  @inline def leader() = sender()

  @inline def voter() = sender() // not explicitly a Raft role, but makes it nice to read
  // end of sender aliases

  def getInternalState: debugger.protocol.State = {

    val recentlyContactedByLeaderVal: String = recentlyContactedByLeader match {
      case Some(x) => x.toString
      case None => "None"
    }
    val color: StateColor = stateName match {
      case Init | Follower => StateColor(1f, 1f, 1f, 0.5f) //white
      case Candidate =>  StateColor(1f, 1f, 0f, 0.5f) //yellow
      case Leader =>  StateColor(0f, 1f, 0f, 0.5f) //green
    }

    State(self.path.name, color,
      vars = //"timeToElection: " + timeToElection._1 + "ms \n" +
        //"replicatedLog: " + replicatedLog + "\n" +
        "replicatedLog: \n" + replicatedLog.prettyPrintLogs + "\n" //+
        //"nextIndex: \n" + nextIndex.prettyPrintMap + "\n" +
        //"matchIndex: \n" + matchIndex.prettyPrintMap + "\n" //+
        //"recentlyContactedByLeader: " +  recentlyContactedByLeaderVal
    ) // from SharedBehaviors trait
  }

}



