package io.quckoo.auth

/**
  * Created by alonsodomin on 15/09/2016.
  */
final case class InvalidPassportException(token: String) extends Exception(s"Token '$token' is not valid.")
