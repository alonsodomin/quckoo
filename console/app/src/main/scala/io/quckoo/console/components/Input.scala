package io.quckoo.console.components

import io.quckoo.time.{MomentJSDate, MomentJSTime}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.annotation.implicitNotFound

/**
  * Created by alonsodomin on 07/04/2016.
  */
object Input {

  @implicitNotFound("Type $A is not supported as Input component")
  sealed trait Converter[A] {
    def from: String => A
    def to: A => String
  }
  object Converter {
    abstract class BaseConverter[A] extends Converter[A] {
      override def to: A => String = _.toString
    }

    implicit val string: Converter[String] = new Converter[String] {
      def to: String => String = identity
      def from: String => String = identity
    }

    implicit val int: Converter[Int] = new BaseConverter[Int] {
      override def from: String => Int = _.toInt
    }

    implicit val long: Converter[Long] = new BaseConverter[Long] {
      override def from: String => Long = _.toLong
    }

    implicit val date: Converter[MomentJSDate] = new BaseConverter[MomentJSDate] {
      override def from: String => MomentJSDate = MomentJSDate.parse
    }

    implicit val time: Converter[MomentJSTime] = new BaseConverter[MomentJSTime] {
      override def from: String => MomentJSTime = MomentJSTime.parse
    }

  }

  @implicitNotFound("Type $A is not supported as Input component")
  sealed abstract class Type[A](val html: String)
  object Type {
    implicit val string = new Type[String]("text") {}
    implicit val int = new Type[Int]("number") {}
    implicit val long = new Type[Long]("number") {}
    implicit val date = new Type[MomentJSDate]("date") {}
    implicit val time = new Type[MomentJSTime]("time") {}
  }

  type OnUpdate[A] = Option[A] => Callback

  case class Props[A](value: Option[A], converter: Converter[A], `type`: Type[A], onUpdate: OnUpdate[A], attrs: Seq[TagMod])

  class Backend[A]($: BackendScope[Props[A], Unit]) {

    def onUpdate(props: Props[A])(evt: ReactEventI): Callback = {
      def convertNewValue: CallbackTo[Option[A]] = CallbackTo {
        if (evt.target.value.isEmpty) None
        else Some(props.converter.from(evt.target.value))
      }

      def propagateChange(value: Option[A]): Callback =
        props.onUpdate(value)

      convertNewValue >>= propagateChange
    }

    def render(props: Props[A]) = {
      <.input(^.`type` := props.`type`.html,
        ^.`class` := "form-control",
        props.value.map(v => ^.value := props.converter.to(v)),
        ^.onChange ==> onUpdate(props),
        ^.onBlur ==> onUpdate(props),
        props.attrs
      )
    }

  }

}

class Input[A: Reusability](onUpdate: Input.OnUpdate[A]) {
  import Input._

  implicit val propsReuse: Reusability[Props[A]] = Reusability.by[Props[A], Option[A]](_.value)
  val reuseConfig = Reusability.shouldComponentUpdate[Props[A], Unit, Backend[A], TopNode]

  val component = ReactComponentB[Props[A]]("Input").
    stateless.
    renderBackend[Backend[A]].
    configure(reuseConfig).
    build

  def apply(value: Option[A], attrs: TagMod*)(implicit C: Converter[A], T: Type[A]) =
    component(Props(value, C, T, onUpdate, attrs))

}
