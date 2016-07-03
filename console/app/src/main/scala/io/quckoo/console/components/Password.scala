package io.quckoo.console.components

import japgolly.scalajs.react.extra.Reusability

/**
  * Created by alonsodomin on 03/07/2016.
  */
final case class Password(value: String) extends AnyVal

object Password {
  implicit val reusability: Reusability[Password] = Reusability.by(_.value)
}
