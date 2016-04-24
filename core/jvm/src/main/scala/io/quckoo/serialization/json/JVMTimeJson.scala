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

package io.quckoo.serialization.json

import java.time.{Instant, ZoneId, ZonedDateTime}

import io.quckoo.time.{DateTime, JDK8DateTime}

import upickle.Js
import upickle.default._

/**
  * Created by alonsodomin on 19/03/2016.
  */
trait JVMTimeJson {

  implicit val writer: Writer[DateTime] = Writer[DateTime] {
    case dateTime =>
      Js.Num(dateTime.toUTC.toEpochMillis)
  }

  implicit val reader: Reader[DateTime] = Reader[DateTime] {
    case Js.Num(millis) =>
      val instant = Instant.ofEpochMilli(millis.toLong)
      val zdt = ZonedDateTime.ofInstant(instant, ZoneId.of("UTC")).
        withZoneSameInstant(ZoneId.systemDefault())
      new JDK8DateTime(zonedDateTime = zdt)
  }

}
