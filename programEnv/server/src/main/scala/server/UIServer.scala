package server

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import java.net.InetSocketAddress

import cmdLine.CmdLineIOProvider
import debugger.protocol.{QueryRequest, QueryResponse}
import debugger.protocol.QueryRequests.QueryRequestJsonFormat
import debugger.protocol.QueryResponses.QueryResponseJsonFormat
import spray.json._

import scala.collection.mutable

/**
  * The debugger UI end connects to this server and sends user requests
  * (The requests are accepted in QueryRequest json format)
  * The TcpServerAcceptingResponses sends Responses to this Server
  */
class UIServer(serverConfig: ServerConfig, debuggerServer: ActorRef) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", serverConfig.uiPort))

  /**
    * Receives QueryRequest from the client (debugger UI)
    * It parses the request and sends it to the TcpServerForDebuggerProcess
    */
  val queryRequestHandler: ActorRef = context.actorOf(RequestHandlerActor.props(debuggerServer), "requestHandler-ServerForUI")

  /**
    * Sends the received QueryResponse (from the TcpServerForDebuggerProcess) to the connected UI clients
    */
  val queryResponseHandler: ActorRef = context.actorOf(ResponseHandlerActor.props, "responseHandler-ServerForUI")

  debuggerServer ! RegisterUIServer(self)

  def receive: Receive = {
    case b @ Bound(localAddress) => println(Console.RED + "Tcp server for debugger UI bound" + Console.RESET)

    case CommandFailed(_: Bind) => context stop self

    // when a client (debugger UI) connects, add it to the list of clients of the responseHandler
    case c @ Connected(remote, local) =>
      val connection = sender()
      connection ! Register(queryRequestHandler) // the incoming requests received from UI will be handled by this actor
      queryResponseHandler ! AddClient(connection)

    // when receives a response from TcpServerForDebuggerProcess, send it to the client
    case queryResponse: QueryResponse =>
      println(Console.RED + "Server received response: " + queryResponse + Console.RESET)
      queryResponseHandler ! queryResponse
      // Display the response on the command line if configured so:
      if(serverConfig.useCmdLine)
        CmdLineIOProvider.putResponse(queryResponse)

    // when receives a request from CmdLine UI port, send it to the debugger
    case queryRequest: QueryRequest =>
      println(Console.RED + "Server received request from CmdLine: " + queryRequest + Console.RESET)
      debuggerServer ! queryRequest
  }
}

object UIServer {
  def props(serverConfig: ServerConfig, debuggerServer: ActorRef): Props = Props(new UIServer(serverConfig, debuggerServer))
}


/**
  * Handles the debugger UI end (client) Requests
  */
class RequestHandlerActor(debuggerServer: ActorRef) extends Actor {
  import Tcp._

  def receive: Receive = {
    case Received(data) =>
      try {
        // The received data must be a QueryRequest
        val request = QueryRequestJsonFormat.read(data.utf8String.parseJson)
        println(Console.RED + "Server received request from network: " + request + Console.RESET)
        debuggerServer ! request

      } catch {
        case e: Exception => println(Console.RED + "Server could not parse the request: " + data.utf8String + Console.RESET)
      }
    case PeerClosed => println("UI Server - Peer closed")
    case _ =>
  }
}

object RequestHandlerActor {
  def props(debuggerServer: ActorRef) = Props(new RequestHandlerActor(debuggerServer))
}

/**
  * Sends the program Responses to the clients
  */
class ResponseHandlerActor extends Actor {
  import Tcp._

  // The client to whom response will be sent (the VR end client)
  private var client: Option[ActorRef] = None

  def receive: Receive = {
    case AddClient(c) =>
      println(Console.RED + "Client added to the ResponseHandler: " + c + Console.RESET)
      client = Some(c)

    case queryResponse: QueryResponse =>
      client match {
        case Some(c) => c ! Write(ByteString(QueryResponseJsonFormat.write(queryResponse).prettyPrint))
        case None => // do nth
      }
    case PeerClosed => println("UI Server - Peer closed")
    case _ =>
  }
}

object ResponseHandlerActor {
  def props = Props(new ResponseHandlerActor())
}

case class AddClient(client: ActorRef)
