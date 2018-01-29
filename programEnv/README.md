debugging-akka-dispatcher
=========================


A custom Akka dispatcher which:

- Logs the actor creation, message send, message receive and actor termination events.
- Reproduces/Enforces a particular delivery order of the messages to the actors. 

The dispatcher intercepts and keeps all actor messages. Before delivering an actor message, it displays the intercepted messages and asks for the user input to select the next delivery.

The user I/O can be communicated through network or the command line depending on the configuration.


### Contents of the repository:

- ````apps```` directory contains example applications to run with the debugging dispatcher
- ````dispatcher```` directory contains the logging and debugging dispatchers
- ````protocol```` directory contains the model classes that are used in the communication of the user query requests and query responses collected from the program under analysis. 
- ````server```` directory contains the TCP servers that are connected by the debugging dispatcher and the UI end 

(The user requests / program responses are communicated to the user by the command line by default. If you configure to use the network, your UI process must connect to the TCP server 
through the port number provided in the configuration file. Your UI process must also send requests / accept responses using the json formats in the protocol directory )


### Building and running an example app with debugging-dispatcher:

Requirements:

- Java 8 SDK
- Scala 2.12
- [Scala Build Tool](http://www.scala-sbt.org/) 

Build the project and publish its libraries locally:

```
sbt compile
sbt publishLocal
```

Run the server for user IO connection to the debugger. The virtual reality debugger user interface connects to that server.

(By default, it uses the command line for IO. Use ``run`` if you will debug your app on the command line without virtual reality visualization.):

```
sbt
project server
run remote
```

Run the example application:

```
cd apps/dining-hakkers
sbt run
```

Make sure that the application connects to the debugging server. Then, run the user interface client.



### Using debugging-dispatcher with your own Akka app:

- Build the project and publish its libraries locally:

```
sbt compile
sbt publishLocal
```

- Add the dispatcher libraries to your project dependencies. 
In your scala build file:

```
resolvers += Resolver.mavenLocal
libraryDependencies ++= Seq(
  "com.debugger" %% "debugging-dispatcher" % "1.0",
  "com.debugger" %% "protocol" % "1.0"
)

```

- Set the default dispatcher of akka actors as DebuggingDispatcher in the configuration file of your project.

```
akka {
  actor {
    default-dispatcher {
      type = akka.dispatch.DebuggingDispatcherConfigurator
    }
  }
}
```

- Configure your options in the debugger.conf file:

   
```
debugging-dispatcher {
  # The name of the trace file for repruducing a particular execution trace 
  traceFile = "traces/starvation.txt"

  # Whether the program has calls to scheduler.schedule (will use virtual time)
  useVirtualTimer = false
  # Virtual time advanced at each step of the Timer, in MILLISECONDS
  # timestep = 1000

  # Log level for the dispatcher logs
  # DEBUG = 0, INFO = 1, WARNING = 2, ERROR = 3
  logLevel = 1

  # Configures 3D visualization settings
  visualization {

    actor {
      # An actor name can be associated to a specific 3D template resource
      # This asset will be used for visualization of this actor
      "philosopher-A" = "Smiley"
      "philosopher-B" = "Smiley"
      "philosopher-C" = "Smiley"
      "philosopher-D" = "Smiley"
      "philosopher-E" = "Smiley"

      "chopstick-1" = "Chopstick"
      "chopstick-2" = "Chopstick"
      "chopstick-3" = "Chopstick"
      "chopstick-4" = "Chopstick"
      "chopstick-5" = "Chopstick"
    }

    topography {
      type = "RING"

      actorList = [
        "chopstick-1", "philosopher-A",
        "chopstick-2", "philosopher-B",
        "chopstick-3", "philosopher-C",
        "chopstick-4", "philosopher-D",
        "chopstick-5", "philosopher-E"]
    }
  }

}
```

- To view a customized state of you actors, make sure that your actors accept GetInternalActorState message and respond with its current state as a State object.




