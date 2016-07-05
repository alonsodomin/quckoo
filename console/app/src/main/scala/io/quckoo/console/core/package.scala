package io.quckoo.console

import diode.{ActionType, Effect}

import scala.concurrent.ExecutionContext
import scala.language.implicitConversions

/**
  * Created by alonsodomin on 05/07/2016.
  */
package object core {

  implicit def action2Effect[A : ActionType](action: => A)(implicit ec: ExecutionContext): Effect =
    Effect.action[A](action)

}
