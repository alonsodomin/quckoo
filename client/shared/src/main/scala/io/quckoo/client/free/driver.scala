package io.quckoo.client.free

import io.quckoo.client.core.Protocol

import scala.concurrent.Future
import scalaz.Kleisli

/**
  * Created by domingueza on 03/11/2016.
  */
object driver {

  trait Driver[P <: Protocol] {

    def send: Kleisli[Future, P#Request, P#Response]
  }



}
