package io.kairos.ui.server.boot

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.stream.ActorMaterializer
import akka.util.Timeout
import io.kairos.ui.server.Server

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 11/10/2015.
 */
object ServerBootstrap extends App {
  implicit val system = ActorSystem("KairosBackend")
  sys.addShutdownHook { system.terminate() }

  implicit val materializer = ActorMaterializer()

  val server = new Server()

  implicit val timeout = Timeout(5 seconds)
  import system.dispatcher

  Http().bindAndHandle(server.router, "0.0.0.0", 8080) map {
    case sb: ServerBinding => println(s"HTTP server started!")
  } recover {
    case ex: Throwable =>
      println("Error starting HTTP server: " + ex.getMessage)
      system.terminate()
  }

}
