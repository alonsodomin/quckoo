/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.components

import java.time.{LocalDate, LocalTime}

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.html_<^._

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
    private abstract sealed class BaseConverter[A] extends Converter[A] {
      override def to: A => String = _.toString
    }

    implicit val string: Converter[String] = new Converter[String] {
      def to: String => String   = identity
      def from: String => String = identity
    }

    implicit val password: Converter[Password] = new Converter[Password] {
      def to: Password => String   = _.value
      def from: String => Password = Password(_)
    }

    implicit val int: Converter[Int] = new BaseConverter[Int] {
      override def from: String => Int = _.toInt
    }

    implicit val long: Converter[Long] = new BaseConverter[Long] {
      override def from: String => Long = _.toLong
    }

    implicit val short: Converter[Short] = new BaseConverter[Short] {
      override def from: String => Short = _.toShort
    }

    implicit val date: Converter[LocalDate] = new BaseConverter[LocalDate] {
      override def from: String => LocalDate = LocalDate.parse
    }

    implicit val time: Converter[LocalTime] = new BaseConverter[LocalTime] {
      override def from: String => LocalTime = LocalTime.parse
    }

  }

  @implicitNotFound("Type ${A} is not supported as Input component")
  sealed abstract class Type[A](val html: String)
  object Type {
    implicit val string   = new Type[String]("text")       {}
    implicit val password = new Type[Password]("password") {}
    implicit val int      = new Type[Int]("number")        {}
    implicit val long     = new Type[Long]("number")       {}
    implicit val short    = new Type[Short]("number")      {}
    implicit val date     = new Type[LocalDate]("date")    {}
    implicit val time     = new Type[LocalTime]("time")    {}
  }

  type OnUpdate[A] = Option[A] => Callback

  final case class Props[A](
      value: Option[A],
      defaultValue: Option[A],
      onUpdate: OnUpdate[A],
      attrs: Seq[TagMod]
  )(implicit val converter: Converter[A], val `type`: Type[A])

  class Backend[A]($ : BackendScope[Props[A], Unit]) {

    def onUpdate(props: Props[A])(evt: ReactEventFromInput): Callback = {
      def convertNewValue: CallbackTo[Option[A]] = CallbackTo {
        if (evt.target.value.isEmpty) None
        else Some(props.converter.from(evt.target.value))
      }

      def propagateChange(value: Option[A]): Callback =
        props.onUpdate(value)

      convertNewValue >>= propagateChange
    }

    def render(props: Props[A]) = {
      def defaultValueAttr: Option[TagMod] =
        props.defaultValue.map(v => ^.defaultValue := props.converter.to(v))
      def valueAttr: TagMod =
        ^.value := props.value.map(v => props.converter.to(v)).getOrElse("")

      <.input(
        ^.`type` := props.`type`.html,
        ^.`class` := "form-control",
        defaultValueAttr.getOrElse(valueAttr),
        ^.onChange ==> onUpdate(props),
        ^.onBlur ==> onUpdate(props),
        props.attrs.toTagMod
      )
    }

  }

  def apply[A: Reusability] = new Input[A]

}

class Input[A: Reusability] private[components] () {
  import Input._

  implicit val propsReuse: Reusability[Props[A]] =
    Reusability.by[Props[A], (Option[A], Option[A])](p => (p.value, p.defaultValue))

  private[components] val component = ScalaComponent
    .builder[Props[A]]("Input")
    .stateless
    .renderBackend[Backend[A]]
    .configure(Reusability.shouldComponentUpdate)
    .build

  def apply(value: Option[A], onUpdate: OnUpdate[A], attrs: TagMod*)(
      implicit C: Converter[A],
      T: Type[A]
  ) =
    component(Props(value, None, onUpdate, attrs))

  def apply(value: Option[A], defaultValue: Option[A], onUpdate: OnUpdate[A], attrs: TagMod*)(
      implicit C: Converter[A],
      T: Type[A]
  ) = component(Props(value, defaultValue, onUpdate, attrs))

}
