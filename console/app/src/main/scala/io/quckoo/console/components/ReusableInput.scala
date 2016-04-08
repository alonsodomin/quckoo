package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalaz._

/**
  * Created by alonsodomin on 07/04/2016.
  */
object ReusableInput {
  import Isomorphism._

  trait Converter[A] extends (A <=> String)
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
  }

  sealed abstract class Type[A](val html: String)
  object Type {
    implicit val string = new Type[String]("text") {}
    implicit val int = new Type[Int]("number") {}
    implicit val long = new Type[Long]("number") {}
  }

  type OnUpdate[A] = Option[A] ~=> Callback

  case class Props[A](value: Option[A], converter: Converter[A], `type`: Type[A], onUpdate: OnUpdate[A])

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
        ^.onChange ==> onUpdate(props)
      )
    }

  }

}
class ReusableInput[A: Reusability](onUpdate: ReusableInput.OnUpdate[A]) {
  import ReusableInput._

  implicit val propsReuse: Reusability[Props[A]] = Reusability.by[Props[A], Option[A]](_.value)
  val reuseConfig = Reusability.shouldComponentUpdateWithOverlay[Props[A], Unit, Backend[A], TopNode]

  val component = ReactComponentB[Props[A]]("Input").
    stateless.
    renderBackend[Backend[A]].
    configure(reuseConfig).
    build

  def create(value: Option[A])(implicit C: Converter[A], T: Type[A]) =
    component(Props(value, C, T, onUpdate))

}
