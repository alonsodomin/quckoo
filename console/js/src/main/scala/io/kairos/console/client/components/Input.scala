package io.kairos.console.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.reactive.Observer

import scalaz.Isomorphism._

/**
  * Created by alonsodomin on 21/02/2016.
  */
object Input {
  type Converter[T] = T <=> String

  abstract class BaseConverter[T] extends Converter[T] {
    override def to: (T) => String = _.toString
  }

  private[this] val StringConverter: Converter[String] = new Converter[String] {
    def to: (String) => String = identity
    def from: (String) => String = identity
  }

  case class Props[T](evar: ExternalVar[T], converter: Converter[T], observer: Observer[T], mods: List[TagMod])
  case class State[T](value: T)

  class InputBackend[T]($: BackendScope[Props[T], State[T]]) {

    def onChange(event: ReactEventI): Callback = {
      def propagateValue(v: T): Callback = $.props.flatMap(p => Callback { p.observer.onNext(v) })

      $.props.map(_.converter.from(event.target.value)).flatMap { value =>
        $.modState(_.copy(value)).flatMap(_ => propagateValue(value))
      }
    }

  }

  def component[T] = ReactComponentB[Props[T]]("Input").
    initialState_P(p => State(p.evar.value)).
    backend(new InputBackend[T](_)).
    renderPS(($, p, s) => {
      val tagMods = Seq(
        ^.value := p.converter.to(s.value),
        ^.`class` := "form-control",
        ^.onChange ==> $.backend.onChange
      ) ++ p.mods
      <.input(tagMods: _*)
    }).
    build

  def text(evar: ExternalVar[String], observer: Observer[String], mods: TagMod*) =
    component[String](Props(evar, StringConverter, observer, (^.tpe := "text") :: mods.toList))

  def password(evar: ExternalVar[String], observer: Observer[String], mods: TagMod*) =
    component[String](Props(evar, StringConverter, observer, (^.tpe := "password") :: mods.toList))

}
