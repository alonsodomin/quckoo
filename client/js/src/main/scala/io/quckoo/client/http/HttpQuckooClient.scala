package io.quckoo.client.http

import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.client.QuckooClientV2
import io.quckoo.client.internal.Request

import scala.concurrent.duration.Duration
import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 06/09/2016.
  */
object HttpQuckooClient extends QuckooClientV2 {
  import channel._

  override def authenticate(username: String, password: String)
                           (implicit ec: ExecutionContext, timeout: Duration): Future[Passport] = {
    val request = Request(Credentials(username, password), timeout, None)
    HttpTransport.channelFor[Credentials].send(request)
  }

}
