package akka.dispatch.io

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.dispatch.{QueryRequestHandler, Settings}
import akka.dispatch.util.CmdLineUtils
import debugger.protocol.QueryRequests.QueryRequestJsonFormat
import debugger.protocol._
import akka.io.Tcp.Connected
import akka.util.ByteString
import debugger.protocol.QueryResponses.QueryResponseJsonFormat
import spray.json._

object NetworkIOProvider extends  IOProvider {
  var clientActor: Option[ActorRef] = None
  var invoker: ActorRef = Actor.noSender

  override def setUp(system: ActorSystem): Unit = {
    /**
      * Communicates with the server via TCPClient
      * Receives QueryResponse from the server to be handled
      * Sends QueryResponse of the running program
      */
    invoker = system.actorOf(Props(new Actor() {
      override def receive: Receive = {
        case Connected(remoteAddr, localAddr) => {
          println("Client received: " + remoteAddr + "  " + localAddr)
        }
        // receives the QueryRequest sent by the Server
        case s: ByteString => {
          val request = QueryRequestJsonFormat.read(s.utf8String.parseJson)
          CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Received user request: " + request)
          QueryRequestHandler.handleRequest(request)
        }
        // when receives a QueryResponse (from the dispatcher), sends it to the server
        case response: QueryResponse => {
          CmdLineUtils.printLog(CmdLineUtils.LOG_DEBUG, "Sending program response: " + response)
          clientActor match {
            case Some(actor) => actor ! ByteString(QueryResponseJsonFormat.write(response).prettyPrint)
            case None => println(Console.RED + "Client actor has not created yet" + Console.RESET)
          }
        }
      }
    }), "DebuggingDispatcherTCPInvoker")

    clientActor = Some(system.actorOf(TcpClient.props(new InetSocketAddress("localhost", Settings.debuggerSettings.debuggerPortNumber), invoker), "DebuggingDispatcherTCPClientActor"))
  }

  override def putResponse(response: QueryResponse): Unit = {
    invoker ! response
  }
}
