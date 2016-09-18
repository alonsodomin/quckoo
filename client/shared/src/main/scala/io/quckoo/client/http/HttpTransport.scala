package io.quckoo.client.http

import io.quckoo.client.core.Transport

/**
  * Created by alonsodomin on 09/09/2016.
  */
trait HttpTransport extends Transport {
  type P = HttpProtocol

  private[client] final val protocol = HttpProtocol
}
