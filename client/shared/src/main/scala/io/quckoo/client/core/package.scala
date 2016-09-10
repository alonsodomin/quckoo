package io.quckoo.client

import scala.concurrent.Future
import scala.util.Try
import scalaz._

/**
  * Created by alonsodomin on 10/09/2016.
  */
package object core {
  type Encoder[In, Out] = Command[In] => Try[Out]
  type Decoder[Out, In] = Out => Try[In]

  object try2Future extends (Try ~> Future) {
    override def apply[A](fa: Try[A]): Future[A] =
      Future.fromTry(fa)
  }

}
