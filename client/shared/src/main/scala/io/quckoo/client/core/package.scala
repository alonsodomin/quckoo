package io.quckoo.client

import scala.concurrent.Future
import scala.util.Try

import scalaz._

/**
  * Created by alonsodomin on 10/09/2016.
  */
package object core {
  type Marshall[Cmd[_] <: Command[_], In, Req] = Cmd[In] => Try[Req]
  type Unmarshall[Res, Rslt] = Res => Try[Rslt]

  object try2Future extends (Try ~> Future) {
    override def apply[A](fa: Try[A]): Future[A] =
      Future.fromTry(fa)
  }

}
