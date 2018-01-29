package server

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}
import akka.util.ByteString
import cmdLine.CmdLineIOProvider
import debugger.protocol.QueryRequest
import debugger.protocol.QueryRequests.QueryRequestJsonFormat
import debugger.protocol.QueryResponses.QueryResponseJsonFormat
import spray.json._

/**
  * The debugger process connects to this server:
  * The debugger process is forwarded the received user requests
  * The responses received from the debugger are passed to the TcpServerForDebuggerUI
  * (The responses from the debugger are accepted in QueryResponse json format)
  */
class DebuggerServer(serverConfig: ServerConfig) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Bind(self, new InetSocketAddress("0.0.0.0", serverConfig.debuggerPort))

  /**
    * Receives QueryResponse (by the connected debugger process) and sends it to the debugger UI
    */
  val queryResponseHandler: ActorRef = context.actorOf(QueryResponseHandlerActor.props, "responseHandler-ServerForDebugger")

  /**
    * Receives QueryRequest (by the DebuggerUIServer) and sends this user request to the connected debugger process
    */
  val queryRequestHandler: ActorRef = context.actorOf(QueryRequestHandlerActor.props, "requestHandler-ServerForDebugger")

  var uiServer: Option[ActorRef] = None

  def receive: Receive = {
    case b @ Bound(localAddress) => println(Console.BLUE + "Tcp server for debugger process bound" + Console.RESET)

    case CommandFailed(_: Bind) => context stop self

    // when a client (debugger process) connects, save this connection in the queryRequestHandler
    case c @ Connected(remote, local) =>
      val connection = sender()
      connection ! Register(queryResponseHandler) // the incoming debugger messages will be handled by this actor
      queryRequestHandler ! AddClient(connection) // the added client will receive the responses
      // Initiates the user input processing after it makes sure that the app/debugger is started:
      if(serverConfig.useCmdLine)
        CmdLineIOProvider.initiate(context.system, uiServer)

    // when receives a request from the DebuggerUIServer, sends it to the debugger process
    case queryRequest: QueryRequest =>
      println(Console.BLUE + "Server received request: " + queryRequest + Console.RESET)
      queryRequestHandler ! queryRequest

    case RegisterUIServer(ref) =>
      uiServer = Some(ref)
      queryResponseHandler ! RegisterUIServer(ref)
  }
}

object DebuggerServer {
  def props(serverConfig: ServerConfig): Props = Props(new DebuggerServer(serverConfig))
}

/**
  * Forwards the debugger UI end (client) Requests to the debugger process
  */
class QueryRequestHandlerActor extends Actor {
  import Tcp._

  // The debugger process (to whom the user request will be sent)
  private var debugger: ActorRef = Actor.noSender

  def receive: Receive = {
    case AddClient(c) =>
      println(Console.BLUE + "Debugger process client set to: " + c + Console.RESET)
      debugger = c

    case queryRequest: QueryRequest =>
      if(!debugger.equals(Actor.noSender)) {
        println(Console.BLUE + "Request to be sent to the debugger: " + QueryRequestJsonFormat.write(queryRequest).prettyPrint + Console.RESET)
        debugger ! Write(ByteString(QueryRequestJsonFormat.write(queryRequest).prettyPrint))
      } else {
        println(Console.RED_B + "Received request before the debugger process connection!")
        println("Request cannot be sent!" + Console.RESET)
      }

    case PeerClosed => println("Debugger server - Peer closed")
    case _ =>
  }
}

object QueryRequestHandlerActor {
  def props = Props(new QueryRequestHandlerActor())
}

/**
  * Receives program output QueryResponse from the debugger process
  * Forwards this response to the TcpServerForDebuggerUI
  */
class QueryResponseHandlerActor() extends Actor {
  import Tcp._

  private var uiServer: Option[ActorRef] = None

  def receive: Receive = {
    case Received(data) =>
      try {
        // The received data must be a QueryResponse
        val response = QueryResponseJsonFormat.read(data.utf8String.parseJson)
        println(Console.BLUE + "Response to be sent to user: " + response + Console.RESET)
        uiServer.foreach(_ ! response)
      } catch {
        case e: Exception => println(Console.BLUE + "Server could not parse the response: " + data.utf8String + Console.RESET)
      }

    case RegisterUIServer(ref) => uiServer = Some(ref)
    case PeerClosed => println("Debugger server - Peer closed")
    case _ => 
  }
}

object QueryResponseHandlerActor {
  def props = Props(new QueryResponseHandlerActor())
}