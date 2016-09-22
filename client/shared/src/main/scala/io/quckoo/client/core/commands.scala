package io.quckoo.client.core

import io.quckoo.auth.Passport

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 08/09/2016.
  */
sealed trait Command[A] {
  val payload: A
  val timeout: FiniteDuration
}

final case class AnonCmd[A](payload: A, timeout: FiniteDuration) extends Command[A]
final case class AuthCmd[A](payload: A, timeout: FiniteDuration, passport: Passport) extends Command[A]
