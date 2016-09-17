package io.quckoo.client

import io.quckoo.client.http.HttpProtocol

/**
  * Created by alonsodomin on 17/09/2016.
  */
private[client] trait Protocols {
  implicit val httpProtocol: HttpProtocol = HttpProtocol
}
