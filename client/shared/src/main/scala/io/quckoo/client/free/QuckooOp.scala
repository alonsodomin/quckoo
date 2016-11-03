package io.quckoo.client.free

import io.quckoo.auth.Passport

import scala.concurrent.duration.FiniteDuration

/**
  * Created by domingueza on 03/11/2016.
  */
trait QuckooOp[A] {
  def timeout: FiniteDuration
}

trait QuckooAuthOp[A] extends QuckooOp[A] {
  def passport: Passport
}
