package akka.dispatch.util

import akka.dispatch.Settings

import scala.collection.immutable.List

object CmdLineUtils {

  def parseInput(range: Range = Range(Int.MinValue, Int.MaxValue), allowedStrs: List[String] = List()): (Int, Option[String]) = {
    val line = Console.readLine().split(" ")

    if (line.isEmpty || line(0).trim().length() == 0 || !line(0).charAt(0).isDigit
      || line(0).toInt < range.start || line(0).toInt > range.end) {
      println("Wrong integer input, try again. ")
      return parseInput(range, allowedStrs)
    }

    val in1 = line(0).toInt
    if (line.size == 1) (in1, None)
    else {
      val st = line(1)
      if (allowedStrs.contains(st)) (in1, Some(st))
      else {
        println("Wrong string input, try again. ")
        parseInput(range, allowedStrs)
      }
    }
  }

  def printLog(logType: Int, s: String): Unit = logType match {
    case LOG_DEBUG if Settings.debuggerSettings.logLevel <= LOG_DEBUG => println(s)
    case LOG_INFO if Settings.debuggerSettings.logLevel <= LOG_INFO => println(s)
    case LOG_WARNING if Settings.debuggerSettings.logLevel <= LOG_WARNING => println(Console.CYAN + s + Console.RESET)
    case LOG_ERROR if Settings.debuggerSettings.logLevel <= LOG_ERROR => println(Console.RED + s + Console.RESET)
    case _ => // do nth
  }

  def printlnForUiInput(s: Any): Unit = {
    println(Console.BLUE + s + Console.RESET)
  }

  /**
    * Fancy printing of a map
    */
  def printMap[K, V](map: Map[K, V]): Unit = map.foreach(a => println(a._1 + "  ==>  " + a._2))

  /**
    * Fancy printing of a map converted to a list
    * (For the case of Actor messages, Key <- ActorRef and ListElement <- Envelope)
    */
  def printListOfMap[Key, ListElement] (l: List[(Key, List[ListElement])], printFunc: (Any => Unit) = println): Unit = {
    ((1 to l.size) zip l).foreach(a => printFunc(a._1 + "  " + a._2._1 + "\n" + a._2._2)) //the main thread adds msg here
  }

  val LOG_DEBUG = 0
  val LOG_INFO = 1
  val LOG_WARNING = 2
  val LOG_ERROR = 3
}
