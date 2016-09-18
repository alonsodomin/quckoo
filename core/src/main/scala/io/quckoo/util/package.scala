package io.quckoo

import scala.concurrent.Future
import scala.util.{Try, Success => StdSuccess, Failure => StdFailure}

import scalaz._
import Scalaz._
import Isomorphism._

/**
  * Created by alonsodomin on 15/09/2016.
  */
package object util {

  type LawfulTry[+A] = Throwable \/ A
  object LawfulTry {
    def apply[A](thunk: => A): LawfulTry[A] =
      \/.fromTryCatchNonFatal(thunk)
  }

  final val lawfulTry2Try = new (LawfulTry ~> Try) {
    override def apply[A](fa: LawfulTry[A]): Try[A] = fa match {
      case -\/(throwable) => StdFailure(throwable)
      case \/-(value)     => StdSuccess(value)
    }
  }

  final val try2lawfulTry = new (Try ~> LawfulTry) {
    override def apply[A](fa: Try[A]): LawfulTry[A] = fa match {
      case StdSuccess(value) => value.right[Throwable]
      case StdFailure(ex)    => ex.left[A]
    }
  }

  final val lawfulTryIso = new (LawfulTry <~> Try) {
    override def to: ~>[LawfulTry, Try] = lawfulTry2Try
    override def from: ~>[Try, LawfulTry] = try2lawfulTry
  }

  final val try2Future = new (Try ~> Future) {
    override def apply[A](fa: Try[A]): Future[A] =
      Future.fromTry(fa)
  }

  final val lawfulTry2Future: LawfulTry ~> Future = lawfulTry2Try andThen try2Future


}
