package io.quckoo.client.http

import io.quckoo.client.core.{Protocol, Transport}

/**
  * Created by alonsodomin on 09/09/2016.
  */
abstract class HttpTransport extends Transport[Protocol.Http] {
  type Request = HttpRequest
  type Response = HttpResponse
}
