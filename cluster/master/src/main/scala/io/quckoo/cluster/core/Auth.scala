/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import io.quckoo.auth.{Passport, Principal, User}
import io.quckoo.serialization.Base64._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait Auth {

  val Realm = "QuckooRealm"
  val secretKey = "dqwjq0jd9wjd192u4ued9hd0ew".getBytes("UTF-8").toBase64

  def basic(credentials: Credentials)(implicit ec: ExecutionContext): Future[Option[Passport]] = {
    credentials match {
      case p @ Credentials.Provided(identifier) =>
        if (identifier == "admin" && p.verify("password")) {
          val passport = generatePassport(User(identifier))
          Future.successful(Some(passport))
        } else Future.successful(None)

      case _ =>
        Future.successful(None)
    }
  }

  def passport(acceptExpired: Boolean = false)(credentials: Credentials)(implicit ec: ExecutionContext): Future[Option[Passport]] = {
    credentials match {
      case p @ Credentials.Provided(token) =>
        if (isValidToken(token)) {
          val claims = token match {
            case JsonWebToken(_, clms, _) =>
              clms.asSimpleMap.toOption

            case _ => None
          }
          Future.successful(claims.map(claimSet => new Passport(claimSet, token)))
        } else {
          Future.successful(None)
        }

      case _ =>
        Future.successful(None)
    }
  }

  def isValidToken(token: String): Boolean = JsonWebToken.validate(token, secretKey)

  def generatePassport(principal: Principal): Passport = {
    val header = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(Map("sub" -> principal.id))

    val jwt = JsonWebToken(header, claimsSet, secretKey)
    // FIXME the claims map must be a multi map
    new Passport(claimsSet.claims.mapValues(_.toString), jwt)
  }

}
