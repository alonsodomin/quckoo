package io.quckoo.auth

import org.scalacheck._

import scalaz._
import Scalaz._

/**
  * Created by domingueza on 11/08/2016.
  */
class XSRFTokenSpec extends Properties("XSRFToken") {
  import Prop._
  import Arbitrary.arbitrary

  val tokens = for {
    userId <- arbitrary[String] if userId.length > 0
    value  <- arbitrary[String] if value.length > 0
  } yield XSRFToken(userId, value)
  implicit lazy val arbitraryToken = Arbitrary(tokens)

  property("serialization") = forAll { (token: XSRFToken) =>
    XSRFToken(token.toString) === token
  }

  property("expire") = forAll { (token: XSRFToken) =>
    token.expire().expired
  }

}
