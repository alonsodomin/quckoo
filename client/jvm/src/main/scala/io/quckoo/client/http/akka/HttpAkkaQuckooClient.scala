package io.quckoo.client.http.akka

import akka.actor.ActorSystem

import io.quckoo.client.QuckooClient
import io.quckoo.client.http._

/**
  * Created by alonsodomin on 21/09/2016.
  */
object HttpAkkaQuckooClient {
  def apply(host: String, port: Int = 80)(implicit actorSystem: ActorSystem = ActorSystem("HttpQuckooClient")) = {
    implicit val backend = new AkkaHttpBackend(host, port)
    QuckooClient[HttpProtocol]
  }
}
