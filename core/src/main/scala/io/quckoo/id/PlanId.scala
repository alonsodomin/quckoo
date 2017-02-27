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

package io.quckoo.id

import java.util.UUID

import upickle.default.{Reader => UReader, Writer => UWriter, _}

/**
  * Created by alonsodomin on 27/02/2017.
  */
final class PlanId private (private val uuid: UUID) extends AnyVal {
  override def toString: String = uuid.toString
}

object PlanId {

  @inline def apply(uuid: UUID): PlanId = new PlanId(uuid)
  @inline def apply(value: String): PlanId = new PlanId(UUID.fromString(value))

  implicit val jsonReader: UReader[PlanId] = UReader[PlanId] {
    implicitly[UReader[UUID]].read andThen PlanId.apply
  }

  implicit val jsonWriter: UWriter[PlanId] = UWriter[PlanId] { planId =>
    implicitly[UWriter[UUID]].write(planId.uuid)
  }

}
