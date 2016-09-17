package io.quckoo.client.http

import io.quckoo.client.core._

/**
  * Created by alonsodomin on 10/09/2016.
  */
final class HttpDriver private (transport: HttpTransport) extends Driver[HttpProtocol](transport)

object HttpDriver {
  def apply(transport: HttpTransport): HttpDriver = new HttpDriver(transport)
}
