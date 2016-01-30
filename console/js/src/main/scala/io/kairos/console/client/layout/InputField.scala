package io.kairos.console.client.layout

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

import scalaz._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object InputField {
  import Isomorphism._

  type Converter[T] = String <=> T
  type Props[T] = (String, String, String, Boolean, Converter[T], ExternalVar[T])

  private[this] def componentB[T] = ReactComponentB[Props[T]]("InputField").
    render_P { case (inputType, id, placeholder, required, converter, field) =>
      val updateField = (event: ReactEventI) => field.set(converter.to(event.target.value))
      <.input(^.id := id,
        ^.`type` := inputType,
        ^.`class` := "form-control",
        ^.required := required,
        ^.placeholder := placeholder,
        ^.value := converter.from(field.value),
        ^.onChange ==> updateField)
    }

  private[this] val stringComp = componentB[String].build
  private[this] val intComp    = componentB[Int].build

  private[this] abstract class NumberConverter[T: Numeric] extends Converter[T] {
    override def from: (T) => String = _.toString
  }

  private[this] val StringConverter: Converter[String] = new Converter[String] {
    def to: (String) => String = identity
    def from: (String) => String = identity
  }

  private[this] val IntConverter: Converter[Int] = new NumberConverter[Int] {
    override def to: (String) => Int = _.toInt
  }

  def text(id: String, placeholder: String, required: Boolean, fieldVar: ExternalVar[String]) =
    stringComp(("text", id, placeholder, required, StringConverter, fieldVar))

  def password(id: String, placeholder: String, required: Boolean, fieldVar: ExternalVar[String]) =
    stringComp(("password", id, placeholder, required, StringConverter, fieldVar))

  def int(id: String, placeholder: String, required: Boolean, fieldVar: ExternalVar[Int]) =
    intComp(("number", id, placeholder, required, IntConverter, fieldVar))

}
