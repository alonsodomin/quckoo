package io.kairos.console.client.components

import io.kairos.{Faults, ValidationFault}
import io.kairos.console.client.validation.Validator
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.concurrent.Implicits.globalScheduler
import monifu.concurrent.cancelables.BooleanCancelable
import monifu.reactive.subjects.PublishSubject

import scala.concurrent.duration._
import scalaz._

/**
  * Created by alonsodomin on 28/02/2016.
  */
object DelayedInput {
  import Isomorphism._

  type Converter[A] = A <=> String

  type OnValid[A] = A => Callback
  type OnInvalid = Faults => Callback

  abstract class BaseConverter[A] extends Converter[A] {
    override def to: A => String = _.toString
  }

  private[this] val StringConverter: Converter[String] = new Converter[String] {
    def to: String => String = identity
    def from: String => String = identity
  }

  case class Props[A](
      initial: A,
      converter: Converter[A],
      validate: Validator[A],
      onValid: OnValid[A],
      onInvalid: OnInvalid,
      tagMods: Seq[TagMod]
  )
  case class State[A](currentValue: A)

  class Backend[A]($: BackendScope[Props[A], State[A]]) extends OnUnmount {
    private[this] val subject = PublishSubject[A]()
    private[this] lazy val stream = subject.debounce(250 millis).publish

    def init(p: Props[A]): Callback = {
      def subscribeHandlers: Callback = Callback {
        stream.map(p.validate).map {
          case Success(value)  => p.onValid(value)
          case Failure(errors) => p.onInvalid(errors)
        } foreach(_.runNow())
      }

      def connect: CallbackTo[BooleanCancelable] =
        CallbackTo { stream.connect() }

      def registerDispose(cancellable: BooleanCancelable): Callback = {
        def cancelConnection: CallbackTo[Boolean] =
          CallbackTo { cancellable.cancel() }

        def completeStream(cancelled: Boolean): Callback = Callback {
          if (cancelled) subject.onComplete()
          else subject.onError(new Exception("Could not cancel event stream"))
        }

        onUnmount(cancelConnection >>= completeStream)
      }

      subscribeHandlers >> connect >>= registerDispose
    }

    def onChange(event: ReactEventI): Callback = {
      def convertValue: CallbackTo[A] =
        $.props.map(_.converter.from(event.target.value))

      def updateState(value: A): CallbackTo[A] =
        $.modState(_.copy(currentValue = value)).ret(value)

      def propagateValue(value: A): Callback =
        Callback { subject.onNext(value) }

      event.preventDefaultCB >> convertValue >>= updateState >>= propagateValue
    }

    def render(p: Props[A], s: State[A]) = {
      val tagMods = Seq(
        ^.value := p.converter.to(s.currentValue),
        ^.`class` := "form-control",
        ^.onChange ==> onChange
      ) ++ p.tagMods

      <.input(tagMods: _*)
    }

  }

  def component[A] = ReactComponentB[Props[A]]("Input").
    initialState_P(p => State(p.initial)).
    renderBackend[Backend[A]].
    componentDidMount($ => $.backend.init($.props)).
    configure(OnUnmount.install).
    build

  def text(initial: String, validate: Validator[String], onValid: OnValid[String], onInvalid: OnInvalid, tagMods: TagMod*) =
    component[String](Props(initial, StringConverter, validate, onValid, onInvalid, (^.tpe := "text") :: tagMods.toList))

}
