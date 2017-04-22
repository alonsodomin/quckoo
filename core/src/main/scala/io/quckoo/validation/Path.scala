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

package io.quckoo.validation

import cats.{Monoid, Show}

import io.circe.{Encoder, Decoder}

/**
  * Created by alonsodomin on 23/10/2016.
  */
sealed trait Path {
  def elements: List[String]
  def ++(other: Path): Path
}

case object PNil extends Path {
  def elements        = Nil
  def ++(other: Path) = other
}

case class PItem(item: String, tail: Path) extends Path {
  def elements        = item :: tail.elements
  def ++(other: Path) = PItem(item, tail ++ other)
}

object Path {
  final val DefaultSeparator = "."

  def apply(elements: String*): Path =
    elements.filter(_.nonEmpty).foldRight(empty)(PItem)

  def empty: Path = PNil

  def parse(path: String, sep: String): Option[Path] = {
    val trimmed = path.trim()
    if (trimmed.isEmpty) Some(PNil)
    else Some(apply(trimmed.split(sep): _*))
  }

  def unapply(path: String): Option[Path] =
    parse(path, s"\\$DefaultSeparator")

  def show(sep: String): Show[Path] = Show.show(_.elements.mkString(sep))

  implicit val pathMonoid = new Monoid[Path] {
    def empty: Path = Path.empty

    def combine(f1: Path, f2: Path): Path = f1 ++ f2
  }

  implicit val pathShow: Show[Path] = show(DefaultSeparator)

  implicit val pathJsonEncoder: Encoder[Path] =
    Encoder[String].contramap(pathShow.show)

  implicit val pathJsonDecoder: Decoder[Path] =
    Decoder[String].emap { str =>
      parse(str, s"\\$DefaultSeparator").map(Right(_)).getOrElse(Left(s"Invalid path: $str"))
    }

}
