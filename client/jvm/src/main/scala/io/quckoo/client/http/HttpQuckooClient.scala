package io.quckoo.client.http

import akka.actor.ActorSystem
import io.quckoo.client.QuckooClientV2

/**
  * Created by alonsodomin on 11/09/2016.
  */
class HttpQuckooClient(host: String, port: Int = 80)(implicit val actorSystem: ActorSystem)
  extends QuckooClientV2(new HttpDriver(new AkkaTransport(host, port)))
