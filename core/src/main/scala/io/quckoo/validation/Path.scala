package io.quckoo.validation

import scalaz.{Monoid, Show}

/**
  * Created by alonsodomin on 23/10/2016.
  */
trait Path {
  def elements: List[String]
  def ++(other: Path): Path
}

case object PNil extends Path {
  def elements = Nil
  def ++(other: Path) = other
}

case class PItem(item: String, tail: Path) extends Path { self =>
  def elements = item :: tail.elements
  def ++(other: Path) = PItem(item, tail ++ other)
}

object Path {
  def apply(element: String): Path = PItem(element, PNil)

  def show(sep: String): Show[Path] = Show.shows(_.elements.mkString(sep))

  implicit val pathMonoid = new Monoid[Path] {
    def zero: Path = PNil

    def append(f1: Path, f2: => Path): Path = f1 ++ f2
  }

  implicit val pathShow = show(".")
}