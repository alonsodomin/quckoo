package io.kairos.console.client.layout

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

import scalaz.{Profunctor, Bifunctor}

/**
  * Created by alonsodomin on 30/01/2016.
  */
object InputField {

  type Converter[T] = (String => T, T => String)
  type Props[T] = (String, String, Boolean, Converter[T], ExternalVar[T])

  private[this] def component[T] = ReactComponentB[Props[T]]("InputField").
    render_P { case (id, placeholder, required, converter, field) =>
      val updateField = (event: ReactEventI) => field.set(converter._1(event.target.value))
      <.input.text(^.id := id,
        ^.`class` := "form-control",
        ^.required := required,
        ^.placeholder := placeholder,
        ^.value := converter._2(field.value),
        ^.onChange ==> updateField)
    } build

  private[this] val StringConverter: Converter[String] = (identity, identity)
  
  def text(id: String, placeholder: String, required: Boolean, fieldVar: ExternalVar[String]) =
    component[String]((id, placeholder, required, StringConverter, fieldVar))

}
