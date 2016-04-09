package io.quckoo.console.scheduler

import io.quckoo.Trigger
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object TriggerSelect {

  object TriggerType extends Enumeration {
    val Immediate, After, Every, At = Value
  }

  val TriggerOption = ReactComponentB[TriggerType.Value]("TriggerOption").
    stateless.
    render_P { triggerType =>
      <.option(^.value := triggerType.id, triggerType.toString())
    } build

  def triggerOptions: Seq[ReactElement] =
    TriggerType.values.toSeq.map(t => TriggerOption.withKey(t.id)(t))

  case class Props(value: Option[Trigger], onUpdate: Option[Trigger] => Callback)
  case class State(selected: Option[TriggerType.Value] = None, value: Option[Trigger])

  class Backend($: BackendScope[Props, State]) {

    def propagateChange: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.value)))

    def onUpdateSelection(evt: ReactEventI): Callback = {
      def refreshNewSelection: CallbackTo[Option[TriggerType.Value]] = {
        val newSelection = {
          if (evt.target.value.isEmpty) None
          else Some(evt.target.value.toInt)
        } map(TriggerType(_))

        $.modState(_.copy(selected = newSelection)).ret(newSelection)
      }

      def updateTriggerValue(selection: Option[TriggerType.Value]): Callback = {
        val trigger = selection.flatMap {
          case TriggerType.Immediate => Some(Trigger.Immediate)
          case _                     => None
        }
        $.modState(_.copy(value = trigger))
      }

      (refreshNewSelection >>= updateTriggerValue) >> propagateChange
    }

    def onUpdateValue[T <: Trigger](value: Option[T]): Callback =
      $.modState(_.copy(value = value), propagateChange)

    def render(props: Props, state: State) = {
      <.div(
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "trigger", "Trigger"),
          <.div(^.`class` := "col-sm-10",
            <.select(^.`class` := "form-control", ^.id := "trigger",
              state.selected.map(v => ^.value := v.id),
              ^.onChange ==> onUpdateSelection,
              triggerOptions
            )
          )
        ),
        state.selected.flatMap {
          case TriggerType.After => Some(AfterTriggerInput(None, onUpdateValue))
          case TriggerType.Every => Some(EveryTriggerInput(None, onUpdateValue))
          case TriggerType.At    => Some(AtTriggerInput(None, onUpdateValue))
          case _                 => None
        }
      )
    }

  }

  val component = ReactComponentB[Props]("TriggerSelect").
    initialState_P(props => State(value = props.value)).
    renderBackend[Backend].
    build

  def apply(value: Option[Trigger], onUpdate: Option[Trigger] => Callback) =
    component(Props(value, onUpdate))

}
