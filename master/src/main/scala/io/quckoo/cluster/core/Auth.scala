/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.core

import akka.http.scaladsl.server.directives.Credentials

import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}

import cats.syntax.option._

import io.quckoo.auth.{Passport, Principal, User}
import io.quckoo.serialization.DataBuffer

import scala.concurrent.Future

/**
  * Created by alonsodomin on 14/10/2015.
  */
trait Auth {

  val Realm = "QuckooRealm"
  private[this] val Right(secretKey) =
    DataBuffer.fromString("dqwjq0jd9wjd192u4ued9hd0ew").toBase64

  def basic(credentials: Credentials): Future[Option[Principal]] = {
    credentials match {
      case p @ Credentials.Provided(identifier) =>
        if (identifier == "admin" && p.verify("password")) {
          Future.successful(User(identifier).some)
        } else Future.successful(none[User])

      case _ =>
        Future.successful(none[User])
    }
  }

  def bearer(acceptExpired: Boolean = false)(
      credentials: Credentials): Future[Option[Passport]] = {
    credentials match {
      case p @ Credentials.Provided(token) =>
        if (isValidToken(token)) {
          val claims = token match {
            case JsonWebToken(_, clms, _) =>
              clms.asSimpleMap.toOption

            case _ => None
          }
          Future.successful(
            claims.map(claimSet => new Passport(claimSet, token)))
        } else {
          Future.successful(None)
        }

      case _ =>
        Future.successful(None)
    }
  }

  def isValidToken(token: String): Boolean =
    JsonWebToken.validate(token, secretKey)

  def generatePassport(principal: Principal): Passport = {
    val header = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(Map("sub" -> principal.id))

    val jwt = JsonWebToken(header, claimsSet, secretKey)
    // FIXME the claims map must be a multi map
    new Passport(claimsSet.claims.mapValues(_.toString), jwt)
  }

}
