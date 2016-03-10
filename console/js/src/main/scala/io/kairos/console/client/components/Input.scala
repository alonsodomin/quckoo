package io.kairos.console.client.components

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
object Input {
  import Isomorphism._

  type Converter[A] = A <=> String

  abstract class BaseConverter[A] extends Converter[A] {
    override def to: A => String = _.toString
  }

  private[this] val StringConverter: Converter[String] = new Converter[String] {
    def to: String => String = identity
    def from: String => String = identity
  }

  private[this] val IntConverter: Converter[Int] = new BaseConverter[Int] {
    override def from: String => Int = _.toInt
  }

  case class Props[A](
      initial: A,
      converter: Converter[A],
      onChange: A => Callback,
      tagMods: Seq[TagMod]
  )
  case class State[A](currentValue: A)

  class Backend[A]($: BackendScope[Props[A], State[A]]) extends OnUnmount {
    private[this] val subject = PublishSubject[A]()
    private[this] lazy val stream = subject.debounce(250 millis).publish

    def init(p: Props[A]): Callback = {
      def subscribeHandlers: Callback = Callback {
        stream.map(p.onChange).foreach(_.runNow())
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

  def text(initial: String, onChange: String => Callback, tagMods: TagMod*) =
    component[String](Props(initial, StringConverter, onChange, (^.tpe := "text") :: tagMods.toList))

  def password(initial: String, onChange: String => Callback, tagMods: TagMod*) =
    component[String](Props(initial, StringConverter, onChange, (^.tpe := "password") :: tagMods.toList))

  def int(initial: Int, onChange: Int => Callback, tagMods: TagMod*) =
    component[Int](Props(initial, IntConverter, onChange, (^.tpe := "number") :: tagMods.toList))

}
