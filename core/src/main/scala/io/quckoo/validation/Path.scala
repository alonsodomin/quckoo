package io.quckoo.validation

import upickle.Js
import upickle.default.{Reader => UReader, Writer => UWriter}

import scalaz.{Monoid, Show}

/**
  * Created by alonsodomin on 23/10/2016.
  */
sealed trait Path {
  def elements: List[String]
  def ++(other: Path): Path
}

case object PNil extends Path {
  def elements = Nil
  def ++(other: Path) = other
}

case class PItem(item: String, tail: Path) extends Path {
  def elements = item :: tail.elements
  def ++(other: Path) = PItem(item, tail ++ other)
}

object Path {
  final val DefaultSeparator = "."

  def apply(elements: String*): Path = elements.foldRight(empty)(PItem)

  def empty: Path = PNil

  def parse(path: String, sep: String): Option[Path] = Some(apply(path.split(sep): _*))

  def unapply(path: String): Option[Path] = parse(path, DefaultSeparator)

  def show(sep: String): Show[Path] = Show.shows(_.elements.mkString(sep))

  implicit val pathMonoid = new Monoid[Path] {
    def zero: Path = empty

    def append(f1: Path, f2: => Path): Path = f1 ++ f2
  }

  implicit val pathShow: Show[Path] = show(DefaultSeparator)

  implicit val pathJsonWriter: UWriter[Path] = UWriter[Path] {
    p => Js.Str(pathShow.shows(p))
  }

  implicit val pathJsonReader: UReader[Path] = UReader[Path] {
    val extract: PartialFunction[Js.Value, String] = {
      case Js.Str(str) => str
    }

    Function.unlift(extract.lift.andThen(_.flatMap(unapply)))
  }

}