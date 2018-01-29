import akka.actor.{Actor, ActorSystem, Props}
import server.{DebuggerServer, RegisterUIServer, ServerConfig, UIServer}


object Main extends App {

  val configName = args.toList match {
    case "remote" :: Nil => "remote.conf"
    case _ => "local.conf"
  }

  val config = new ServerConfig(configName)
  val system = ActorSystem("sys")

  val debuggingServer = system.actorOf(DebuggerServer.props(config), "TcpServerForDebuggerProcess")
  val uiServer = system.actorOf(UIServer.props(config, debuggingServer), "TcpServerForDebuggerUI")

  // Note: Make sure that debugging process is connected before debugging UI is connected
  // CmdLine UI is initiated once debugging process is connected

  println("Using the configuration file: " + configName)

}
