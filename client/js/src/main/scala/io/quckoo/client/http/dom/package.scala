package io.quckoo.client.http

import io.quckoo.client.QuckooClient
import io.quckoo.client.core.DriverBackend

/**
  * Created by alonsodomin on 20/09/2016.
  */
package object dom {

  implicit val backend: DriverBackend[HttpProtocol] = HttpDOMBackend
  val HttpDOMQuckooClient = QuckooClient[HttpProtocol]

}
