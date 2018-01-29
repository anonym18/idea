package server

import com.typesafe.config.ConfigFactory

import scala.util.Try

class ServerConfig(configFileName: String) {

  private[this] val config = ConfigFactory.load(configFileName)

  /**
    *  Set to true if command line will be used for user IO
    *  Configured from the conf file
    */
  val useCmdLine: Boolean = Try(config.getString("debugging-server.ioChoice")).getOrElse("") equalsIgnoreCase "cmdline"

  val debuggerPort: Int = Try(config.getInt("debugging-server.debuggerPort")).getOrElse(5555)

  val uiPort: Int = Try(config.getInt("debugging-server.uiPort")).getOrElse(2772)
}
