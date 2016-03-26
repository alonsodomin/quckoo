package io.quckoo.console.client.components

import io.quckoo.Validated
import io.quckoo.console.client.validation._
import io.quckoo.fault.Faults
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.OnUnmount
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.concurrent.Implicits.globalScheduler
import monifu.concurrent.cancelables.BooleanCancelable
import monifu.reactive.Ack
import monifu.reactive.observables.ConnectableObservable
import monifu.reactive.subjects.PublishSubject

import scala.concurrent.duration._
import scalaz._

/**
  * Created by alonsodomin on 25/02/2016.
  */
object ValidatedInput {
  import Isomorphism._

  type OnChange[T] = Validated[T] => Callback
  type OnValid[A] = A => Callback
  type OnInvalid = Faults => Callback

  type Converter[T] = T <=> String

  abstract class BaseConverter[T] extends Converter[T] {
    override def to: (T) => String = _.toString
  }

  private[this] val StringConverter: Converter[String] = new Converter[String] {
    def to: (String) => String = identity
    def from: (String) => String = identity
  }

  case class Props[T](
       id: String,
       label: Option[String],
       initial: T,
       converter: Converter[T],
       validator: Validator[T],
       onValid: OnValid[T],
       onInvalid: OnInvalid,
       tagMods: Seq[TagMod]
  )
  case class State[T](input: T, validated: Option[Validated[T]] = None)

  class Backend[T]($: BackendScope[Props[T], State[T]]) extends OnUnmount {
    private[this] val subject = PublishSubject[Validated[T]]()

    private lazy val stream: ConnectableObservable[Validated[T]] =
      subject.debounce(500 millis).publish

    def init: Callback = {
      def subscribeSelf: Callback = Callback {
        stream.subscribe { v =>
          $.modState(_.copy(validated = Some(v))).runNow()
          Ack.Continue
        }
      }

      def subscribeObserver: Callback = {
        $.props.map(p => stream.map {
          case Success(value) => p.onValid(value)
          case Failure(errors) => p.onInvalid(errors)
        } foreach(_.runNow()))
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

      subscribeSelf >> subscribeObserver >> connect >>= registerDispose
    }

    def onChange(event: ReactEventI): Callback = {
      def convertValue: CallbackTo[T] =
        $.props.map(_.converter.from(event.target.value))

      def updateState(value: T): CallbackTo[T] =
        $.modState(_.copy(input = value)).ret(value)

      def validateValue(value: T): CallbackTo[Validated[T]] =
        $.props.map(_.validator(value))

      def propagateValidation(validated: Validated[T]): Callback =
        Callback { subject.onNext(validated) }

      convertValue >>= updateState >>= validateValue >>= propagateValidation
    }

    def render(p: Props[T], s: State[T]) = {
      val tagMods = Seq(
        ^.id := p.id,
        ^.value := p.converter.to(s.input),
        ^.`class` := "form-control",
        ^.onChange ==> onChange
      ) ++ p.tagMods

      <.div(^.classSet1("form-group", "has-error" -> s.validated.exists(_.isFailure)),
        p.label.map(text => <.label(^.`class` := "control-label", ^.`for` := p.id, text)),
        <.input(tagMods: _*),
        s.validated.flatMap(_.swap.map(_.list.toList).toOption).map { errors =>
          <.span(^.`class` := "help-block with-errors", errors.map(_.toString()))
        }
      )
    }

  }

  def component[T] = ReactComponentB[Props[T]]("ValidatedInput").
    initialState_P(p => State(p.initial)).
    renderBackend[Backend[T]].
    componentDidMount(_.backend.init).
    configure(OnUnmount.install).
    build

  def text(id: String, label: Option[String], initial: String, validator: Validator[String], onValid: OnValid[String], onInvalid: OnInvalid, tagMods: TagMod*) =
    component[String](Props(id, label, initial, StringConverter, validator, onValid, onInvalid, (^.tpe := "text") :: tagMods.toList))

}
