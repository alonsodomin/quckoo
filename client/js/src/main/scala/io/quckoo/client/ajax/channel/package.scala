package io.quckoo.client.ajax

import io.quckoo.auth.{Credentials, Passport}
import io.quckoo.client.internal.channel.{ChannelFactory, Protocol}
import io.quckoo.serialization.Base64._

import org.scalajs.dom.ext.Ajax

import scala.concurrent.ExecutionContext

/**
  * Created by alonsodomin on 06/09/2016.
  */
package object channel {

  implicit def authChannelFactory[P <: Protocol](implicit ec: ExecutionContext, ev: P =:= Protocol.Http) =
    ChannelFactory.simple[Protocol.Http, Credentials, Passport] { reqCtx =>
      import reqCtx.payload._

      val authentication = s"$username:$password".getBytes("UTF-8").toBase64
      val hdrs = Map(AuthorizationHeader -> s"Basic $authentication")

      Ajax.post("").map(res => new Passport(res.responseText))
    }

}
