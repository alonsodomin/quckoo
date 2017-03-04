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

import java.io.File
import java.nio.file.Paths

import pureconfig._
import pureconfig.error._

import scala.util.Try
import scalaz.NonEmptyList

/**
  * Created by alonsodomin on 04/11/2016.
  */
package object config {

  implicit def hint[A]: ProductHint[A] = ProductHint(ConfigFieldMapping(CamelCase, KebabCase))

  implicit val fileConfigConvert: ConfigConvert[File] =
    ConfigConvert.fromStringConvert(
      ConfigConvert.tryF(p => Try(Paths.get(p)).map(_.toAbsolutePath.toFile)),
      _.getAbsolutePath
    )

  def describeConfigFailures(configFailures: ConfigReaderFailures): List[String] = {
    def describeLocation(location: Option[ConfigValueLocation]): String =
      location.map(loc => s"${loc.description} :: ").getOrElse("")

    def prependLocation(msg: String, location: Option[ConfigValueLocation]): String =
      describeLocation(location) + msg

    configFailures.toList.map {
      case CannotConvertNull =>
        s"Can not convert null value"

      case CannotConvert(value, toTyp, because, location) =>
        prependLocation(s"Can not convert value '$value' to type $toTyp because $because", location)

      case CollidingKeys(key, existingValue, location) =>
        prependLocation(s"Key '$key' collides in existing value: $existingValue", location)

      case KeyNotFound(key, location) =>
        prependLocation(s"Key '$key' not found", location)

      case UnknownKey(key, location) =>
        prependLocation(s"Unknow key '$key'", location)

      case WrongType(found, expected, location) =>
        prependLocation(s"Found type '$found' but expected type '$expected'", location)

      case WrongTypeForKey(found, expected, key, location) =>
        prependLocation(s"Found type '$found' for key '$key' but expected '$expected'", location)

      case ThrowableFailure(ex, location) =>
        prependLocation(s"Threw exception: ${ex.getMessage}", location)

      case EmptyStringFound(typ, location) =>
        prependLocation(s"Found empty string for type '$typ'", location)

      case NoValidCoproductChoiceFound(value, location) =>
        prependLocation(s"No valid coproduct choice found in value: $value", location)
    }
  }

}
