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

package io.quckoo

import java.util.UUID

import upickle.default.{Reader => UReader, Writer => UWriter, _}

import scalaz.{Equal, Show}

/**
  * Created by alonsodomin on 27/02/2017.
  */
final class PlanId private (private val uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

object PlanId {

  @inline def apply(uuid: UUID): PlanId = new PlanId(uuid)
  @inline def apply(value: String): PlanId = new PlanId(UUID.fromString(value))

  // Upickle encoders

  implicit val jsonReader: UReader[PlanId] = UReader[PlanId] {
    implicitly[UReader[String]].read andThen PlanId.apply
  }

  implicit val jsonWriter: UWriter[PlanId] = UWriter[PlanId] { planId =>
    implicitly[UWriter[String]].write(planId.uuid.toString)
  }

  // Typeclass instances

  implicit val planIdEq: Equal[PlanId] = Equal.equal((lhs, rhs) => lhs.uuid.equals(rhs.uuid))
  implicit val planIdShow: Show[PlanId] = Show.showFromToString

}
