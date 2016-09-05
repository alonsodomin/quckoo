package io.quckoo.client.internal

import io.quckoo.auth.Passport

import scala.concurrent.duration.Duration

/**
  * Created by alonsodomin on 05/09/2016.
  */
final case class Request[A](payload: A, timeout: Duration, passport: Option[Passport])