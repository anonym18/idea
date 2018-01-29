package akka.dispatch

import java.util.concurrent.TimeUnit

import com.typesafe.config.{Config, ConfigFactory}

import collection.JavaConverters._
import scala.concurrent.duration.FiniteDuration

case class DebuggerSettings (
  debuggerConfigFile: String = "debugger.conf",
  askTimeOutMsec: Int = 5000, // used when the internal state of an actor is asked
  debuggerPortNumber: Int = 5555,
  replayTraceFile: Option[String],
  matchMsgContent: Boolean = false, // false does not match all message content but sender and receiver (e.g. when conytent has timestamps etc)
  noInterceptMsgs: List[String] = List(),
  actorStateBy: String = "none",
  logLevel: Int,
  useTimer: Boolean,
  timeStep: FiniteDuration
)

case class VisualizationSettings (
  topographyType: Option[String],
  topographyActorList: Option[List[String]]
)

case class ActorSettings (
  /* The 3D image resource id to visualize the actor*/
  visualResourceId: Option[String],
  /* The position where the actor will be visualized*/
  position: Option[(Int, Int, Int)],
  /* True - do not visualize the messages received by this actor */
  isSuppressed: Boolean,
  /* True if the user provides the state as an answer to GetInternalActorState message.
     If false, the actor state is read by reflection. */
  isStateProvided: Boolean
)

object Settings {
  private val debuggerConfigFile: String = "debugger.conf"
  private val debuggerPortPath = "debugging-dispatcher.debuggerPort"
  private val debuggerReplayTraceFilePath = "debugging-dispatcher.traceFile"
  private val matchMsgContentPath = "debugging-dispatcher.matchMsgContent"
  private val noInterceptMsgsPath = "debugging-dispatcher.noInterceptMsgs"
  private val actorStateByPath = "debugging-dispatcher.actorStateBy"
  private val useTimerPath = "debugging-dispatcher.useVirtualTimer"
  private val timeStepPath = "debugging-dispatcher.timestep"
  private val topographyTypePath = "debugging-dispatcher.visualization.topography.type"
  private val topographyActorListPath = "debugging-dispatcher.visualization.topography.actorList"
  private val actorVisualResourcePathPrefix = "debugging-dispatcher.visualization.actor."
  private val logLevelPath = "debugging-dispatcher.logLevel"

  private val defaultDebuggerPortNumber = 5555 // default port number to communicate to TCPServerForDebuggingProcess
  // none - no actor state, reflection - read by reflection, message - read by GetInternalState
  private val actorStateByVals = Set("none", "reflection", "message")

  private val config: Config = ConfigFactory.load(debuggerConfigFile)

  private val debuggerSettingsFromFile = DebuggerSettings (
    debuggerPortNumber = if(config.hasPath(debuggerPortPath)) {
      val in = config.getString(debuggerPortPath).toInt
      if(in >= 1 && in <= 65535) in
      else {
        println("The config has an invalid port number for the server. Using " + defaultDebuggerPortNumber + " by default.")
        defaultDebuggerPortNumber
      }
    } else defaultDebuggerPortNumber,

    replayTraceFile = if(config.hasPath(debuggerReplayTraceFilePath)) Some(config.getString(debuggerReplayTraceFilePath)) else None,

    matchMsgContent = if(config.hasPath(matchMsgContentPath)) config.getBoolean(matchMsgContentPath) else false,

    noInterceptMsgs = if(config.hasPath(noInterceptMsgsPath)) config.getStringList(noInterceptMsgsPath).asScala.toList else List(),

    actorStateBy = if(config.hasPath(actorStateByPath)) {
      val str = config.getString(actorStateByPath)
      if(actorStateByVals.contains(str.toLowerCase)) str.toLowerCase else "none"
    } else "none",

    logLevel = if(config.hasPath(logLevelPath)) config.getInt(logLevelPath) else 1, //CmdLineUtils.LOG_INFO

    useTimer = if(config.hasPath(useTimerPath)) config.getBoolean(useTimerPath) else false,

    timeStep = if(config.hasPath(timeStepPath)) FiniteDuration(config.getDuration(timeStepPath, TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS)
      else {
        println("No valid timestep duration provided in configuration file. Using default timestep 1 MILLISECONDS")
        FiniteDuration(1, TimeUnit.MILLISECONDS)
      }
  )

  private val visualizationSettingsFromFile = VisualizationSettings(
    topographyType = if(config.hasPath(topographyTypePath)) Some(config.getString(topographyTypePath)) else None,

    topographyActorList = if(config.hasPath(topographyActorListPath)) Some(config.getStringList(topographyActorListPath).asScala.toList) else None
  )

  def actorSettingsFromFile(actorName: String): ActorSettings = ActorSettings(
    visualResourceId = {
      val path = actorVisualResourcePathPrefix + actorName
      if(config.hasPath(path)) Some(config.getString(path))
      else None
    },
    position = None, //todo
    isSuppressed = false, //todo
    isStateProvided = false // todo
  )

  var debuggerSettings: DebuggerSettings = debuggerSettingsFromFile //todo private
  var visualizationSettings: VisualizationSettings = visualizationSettingsFromFile
  var actorSettings: (String => ActorSettings) = actorSettingsFromFile

  /**
    * User methods for programmatically setting the options
    */
  def setLogLevel(level: Int): Unit = debuggerSettings = debuggerSettings.copy(logLevel = level)

  def setUseTimer(useTimer: Boolean): Unit = debuggerSettings = debuggerSettings.copy(useTimer = useTimer)

  def setTimeStep(step: FiniteDuration): Unit = debuggerSettings = debuggerSettings.copy(timeStep = step)

  def setTopographyType(topType: String): Unit = visualizationSettings = visualizationSettings.copy(topographyType = Some(topType))

  // todo insert into list, etc
  def setTopographyList(list: List[String]): Unit = visualizationSettings = visualizationSettings.copy(topographyActorList = Some(list))
}
