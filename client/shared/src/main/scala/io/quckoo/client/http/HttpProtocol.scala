package io.quckoo.client.http

import io.quckoo.client.core.Protocol

/**
  * Created by alonsodomin on 17/09/2016.
  */
trait HttpProtocol extends Protocol {
  type Request = HttpRequest
  type Response = HttpResponse
}