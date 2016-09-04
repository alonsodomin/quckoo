package io.quckoo.auth

import org.scalacheck._
import org.scalatest.{FunSuite, Matchers}
import org.scalatest.prop.GeneratorDrivenPropertyChecks

/**
  * Created by domingueza on 11/08/2016.
  */
class XSRFTokenSpec extends FunSuite with GeneratorDrivenPropertyChecks with Matchers {

  val tokens = for {
    userId <- Gen.alphaStr if userId.length > 0
    value  <- Gen.alphaStr if value.length > 0
  } yield XSRFToken(userId, value)
  implicit lazy val arbitraryToken = Arbitrary(tokens)

  test("serialization") {
    forAll { (token: XSRFToken) =>
      XSRFToken(token.toString) shouldBe token
    }
  }

  test("expiration") {
    forAll { (token: XSRFToken) =>
      token.expire().expired shouldBe true
    }
  }

}
