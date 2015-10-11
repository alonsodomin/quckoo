package io.kairos.ui

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 11/10/2015.
 */
object Main extends App with KairosHttpService {

  implicit val system = ActorSystem("KairosBackend")
  implicit val materializer = ActorMaterializer()

  implicit val timeout = Timeout(5 seconds)

  val bindingFuture = Http().bindAndHandle(router, "0.0.0.0", 8080)

}
