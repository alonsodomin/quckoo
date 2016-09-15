package io.quckoo

import scala.concurrent.Future
import scala.util.{Try, Success => StdSuccess, Failure => StdFailure}
import scalaz._

/**
  * Created by alonsodomin on 15/09/2016.
  */
package object util {

  type TryE[+A] = Throwable \/ A

  object TryE {
    def apply[A](thunk: => A): TryE[A] =
      \/.fromTryCatchNonFatal(thunk)
  }

  final val either2Try = new (TryE ~> Try) {
    override def apply[A](fa: TryE[A]): Try[A] = fa match {
      case -\/(throwable) => StdFailure(throwable)
      case \/-(value)     => StdSuccess(value)
    }
  }

  final val try2Future = new (Try ~> Future) {
    override def apply[A](fa: Try[A]): Future[A] =
      Future.fromTry(fa)
  }

  final val either2Future = either2Try andThen try2Future

}
