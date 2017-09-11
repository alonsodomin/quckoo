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

import akka.actor.{Actor, ActorLogging, Props}

import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}

import io.quckoo.auth.{Passport, Session, Subject, SubjectId, User}
import io.quckoo.serialization.DataBuffer

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 14/10/2015.
  */
object UserAuthenticator {

  private val Right(secretKey) =
    DataBuffer.fromString("dqwjq0jd9wjd192u4ued9hd0ew").toBase64

  case class Authenticate(username: String, password: Array[Char])
  case class AuthenticationSuccess(session: Session)
  case object AuthenticationFailed

  case class RefreshToken(subjectId: SubjectId)

  def props(sessionTimeout: FiniteDuration): Props =
    Props(classOf[UserAuthenticator], sessionTimeout)

}

class UserAuthenticator(sessionTimeout: FiniteDuration) extends Actor with ActorLogging {
  import UserAuthenticator._

  def receive = running(Map.empty, Map.empty)

  private[this] def running(
      subjects: Map[String, SubjectId],
      sessions: Map[SubjectId, Session]
  ): Receive = {
    case Authenticate(username, password) =>
      context.become(running(subjects, sessions))
  }

  private[this] def generatePassport(subject: Subject): Passport = {
    val header    = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(Map("sub" -> subject.id.value))

    val jwt = JsonWebToken(header, claimsSet, secretKey)
    // FIXME the claims map must be a multi map
    new Passport(claimsSet.claims.mapValues(_.toString), jwt)
  }

}
