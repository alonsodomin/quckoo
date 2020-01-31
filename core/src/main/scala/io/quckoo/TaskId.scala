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

package io.quckoo

import java.util.UUID

import cats.{Eq, Show}

import io.circe.{Decoder, Encoder, Codec, KeyDecoder, KeyEncoder}

import scala.util.Try

/**
  * Created by alonsodomin on 27/02/2017.
  */
final class TaskId private (private val uuid: UUID) extends AnyVal {

  override def toString: String = uuid.toString

}

object TaskId {

  @inline def apply(uuid: UUID): TaskId = new TaskId(uuid)

  // Circe encoding/decoding

  implicit val circeTaskIdCodec: Codec[TaskId] =
    Codec.from(Decoder[UUID].map(apply), Encoder[UUID].contramap(_.uuid))

  implicit val circeTaskIdKeyEncoder: KeyEncoder[TaskId] =
    KeyEncoder.instance(_.uuid.toString)

  implicit val circeTaskIdKeyDecoder: KeyDecoder[TaskId] =
    KeyDecoder.instance(id => Try(UUID.fromString(id)).map(apply).toOption)

  // Typeclass instances

  implicit val taskIdEq: Eq[TaskId]     = Eq.instance((lhs, rhs) => lhs.uuid.equals(rhs.uuid))
  implicit val taskIdShow: Show[TaskId] = Show.fromToString

}
