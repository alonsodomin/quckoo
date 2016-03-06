package io.kairos.console.client.layout

import io.kairos.{ValidationFault, Fault, Required, Validated}
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.language.implicitConversions
import scalaz._

/**
  * Created by alonsodomin on 31/01/2016.
  */
object FormField {
  import Isomorphism._

  // Validation for values

  type Validator[T] = T => Validated[T]

  private[this] def noOpValidator[T]: Validator[T] = {
    import Scalaz._
    _.successNel[Fault]
  }

  def notEmptyStr(fieldId: String)(str: String): Validated[String] = {
    import Scalaz._
    if (str.isEmpty) Required(fieldId).asInstanceOf[Fault].failureNel[String]
    else str.successNel[Fault]
  }

  // Isomorphisms between data types and string values

  type Converter[T] = T <=> String

  abstract class BaseConverter[T] extends Converter[T] {
    override def to: (T) => String = _.toString
  }

  private[this] val StringConverter: Converter[String] = new Converter[String] {
    def to: (String) => String = identity
    def from: (String) => String = identity
  }
  private[this] val IntConverter: Converter[Int] = new BaseConverter[Int] {
    override def from: String => Int = _.toInt
  }

  // Data Accessor

  type Getter[T] = CallbackTo[T]
  type Setter[T] = T => Callback

  case class Accessor[T](getter: Getter[T], setter: Setter[T])

  object Accessor {
    implicit def fromExternalVar[T](externalVar: ExternalVar[T]): Accessor[T] =
      Accessor(CallbackTo { externalVar.value }, externalVar.set)
  }

  case class Props[T](
    inputType: String,
    id: String,
    label: Option[String],
    placeholder: Option[String],
    converter: Converter[T],
    validator: Validator[T],
    accessor: Accessor[T]
  )

  case class State[T](notification: Option[Notification] = None, valid: Boolean = true, value: T)

  class FieldBackend[T]($: BackendScope[Props[T], State[T]]) {

    def validate(value: T): CallbackTo[Validated[T]] =
      $.props.map(_.validator(value))

    def updateField(event: ReactEventI): Callback = {
      $.props.map(_.converter.from(event.target.value)).flatMap { value =>
        $.modState(_.copy(value = value), $.state.flatMap(s => validate(s.value)).flatMap {
          case Failure(errors) =>
            $.modState(_.copy(notification = Some(Notification.danger(errors.head)), valid = false))
          case Success(_) =>
            $.modState(_.copy(notification = None, valid = true))
        }) >> $.props.flatMap(p => p.accessor.setter(value))
      }
    }

  }

  private[this] def componentB[T] = ReactComponentB[Props[T]]("FormField").
    initialState_P(props => State(value = props.accessor.getter.runNow())).
    backend(new FieldBackend[T](_)).
    renderPS(($, props, state) => {
      <.div(^.classSet1("form-group", "has-error" -> !state.valid),
        props.label.map { text =>
          <.label(^.`for` := props.id, ^.`class` := "col-sm-2 control-label", text)
        }.getOrElse(EmptyTag),
        <.div(^.classSet("col-sm-10" -> props.label.isDefined),
          <.input(^.id := props.id,
            ^.`type` := props.inputType,
            ^.classSet1("form-control", "has-error" -> !state.valid),
            ^.placeholder := props.placeholder.getOrElse(""),
            ^.value := props.converter.to(state.value),
            ^.onChange ==> $.backend.updateField
          ),
          if (!state.valid)
            NotificationDisplay(state.notification.toList)
          else EmptyTag
        )
      )
    })

  private[this] val componentS = componentB[String].build

  def text(id: String, label: Option[String] = None,
      placeholder: Option[String] = None,
      validator: Validator[String] = noOpValidator[String],
      accessor: Accessor[String]) =
    componentS(Props("text", id, label, placeholder, StringConverter, validator, accessor))

  def password(id: String, label: Option[String] = None,
      placeholder: Option[String] = None,
      validator: Validator[String] = noOpValidator[String],
      accessor: Accessor[String]) =
    componentS(Props("password", id, label, placeholder, StringConverter, validator, accessor))

}
