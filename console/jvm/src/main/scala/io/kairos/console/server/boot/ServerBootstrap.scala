package io.kairos.console.server.boot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.kairos.console.server.Server
import org.slf4s.Logging

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 11/10/2015.
 */
object ServerBootstrap extends App with Logging {
  implicit val system = ActorSystem("KairosBackend")
  sys.addShutdownHook { system.terminate() }

  implicit val materializer = ActorMaterializer()

  val server = new Server()

  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher

  Http().bindAndHandle(server.router, "0.0.0.0", 8080) map {
    case sb: ServerBinding => log.info(s"HTTP server started!")
  } recover {
    case ex: Throwable =>
      log.error("Error starting HTTP server." + ex)
      system.terminate()
  }

}
