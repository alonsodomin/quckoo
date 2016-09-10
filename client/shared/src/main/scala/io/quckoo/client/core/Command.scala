package io.quckoo.client.core

import io.quckoo.auth.Passport

import scala.concurrent.duration.Duration

/**
  * Created by alonsodomin on 08/09/2016.
  */
final case class Command[A](payload: A, timeout: Duration, passport: Option[Passport])