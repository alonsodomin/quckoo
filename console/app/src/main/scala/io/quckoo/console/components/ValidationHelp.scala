package io.quckoo.console.components

import io.quckoo.Validated
import io.quckoo.console.validation._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.prefix_<^._

import monifu.concurrent.Implicits.globalScheduler
import monifu.concurrent.Cancelable
import monifu.reactive.Ack.Continue
import monifu.reactive.{Observable, Observer}
import org.scalajs.dom.html.Span

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 21/02/2016.
  */
object ValidationHelp {

  case class Props[T](observable: Observable[T], validator: Validator[T], observer: Observer[Boolean])
  case class State[T](valid: Option[Validated[T]])

  class ValidationBackend[T]($: BackendScope[Props[T], State[T]]) extends OnUnmount { self =>

    def init: Callback = {
      def subscribe: CallbackTo[Cancelable] = $.props.map(p => {
        p.observable.debounce(750 millis).subscribe(v => {
          self.validate(v).runNow()
          Continue
        })
      })

      def dispose(subscription: Cancelable): Callback =
        onUnmount(Callback { subscription.cancel() })

      subscribe >>= dispose
    }

    def validate(t: T): Callback = {
      def invokeValidator: CallbackTo[Validated[T]] =
        $.props.map(_.validator(t))

      def updateState(valid: Validated[T]): CallbackTo[Validated[T]] =
        $.modState(_.copy(valid = Some(valid))).flatMap(_ => CallbackTo.pure(valid))

      def propagateValid(valid: Validated[T]): Callback =
        $.props.map(_.observer.onNext(valid.isSuccess))

      invokeValidator >>= updateState >>= propagateValid
    }

  }

  def component[T] = ReactComponentB[Props[T]]("Validation").
    initialState(State[T](None)).
    backend(new ValidationBackend[T](_)).
    render($ => {
      $.state.valid.flatMap(_.swap.map(_.list.toList).toOption).map { errors =>
        <.span(^.`class` := "help-block with-errors", errors.map(_.toString()))
      }.getOrElse[ReactTagOf[Span]](<.span(EmptyTag))
    }).
    componentDidMount(_.backend.init).
    configure(OnUnmount.install).
    build

  def apply[T](observable: Observable[T], validator: Validator[T], observer: Observer[Boolean]) =
    component[T](Props(observable, validator, observer))

}
