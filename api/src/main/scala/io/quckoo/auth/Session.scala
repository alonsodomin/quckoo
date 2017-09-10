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

package io.quckoo.auth

import cats.Monoid

sealed trait Session extends Product with Serializable

object Session {
  def passportFrom(session: Session): Option[Passport] = session match {
    case Authenticated(passport) => Some(passport)
    case _                       => None
  }

  sealed trait Anonymous extends Session
  case object Anonymous extends Anonymous

  final case class Authenticated(passport: Passport) extends Session

  implicit val sessionMonoid: Monoid[Session] = new Monoid[Session] {
    override def empty = Anonymous

    override def combine(x: Session, y: Session) = y
  }

}
