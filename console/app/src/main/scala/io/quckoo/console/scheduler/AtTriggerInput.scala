package io.quckoo.console.scheduler

import io.quckoo.Trigger
import io.quckoo.console.components._
import io.quckoo.time.{MomentJSDate, MomentJSDateTime, MomentJSTime}
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object AtTriggerInput {

  case class Props(value: Option[Trigger.At], onUpdate: Option[Trigger.At] => Callback)
  case class State(date: Option[MomentJSDate], time: Option[MomentJSTime])

  class Backend($: BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val value = $.state.map(st => st.date.flatMap(date => st.time.map(time => (date, time))))
      value.flatMap {
        case Some((date, time)) =>
          val dateTime = new MomentJSDateTime(date, time)
          val trigger = Trigger.At(dateTime)
          $.props.flatMap(_.onUpdate(Some(trigger)))

        case _ =>
          $.props.flatMap(_.onUpdate(None))
      }
    }

    def onDateUpdate(value: Option[MomentJSDate]): Callback =
      $.modState(_.copy(date = value)) >> propagateUpdate

    def onTimeUpdate(value: Option[MomentJSTime]): Callback =
      $.modState(_.copy(time = value)) >> propagateUpdate

    val DateInput = new ReusableInput[MomentJSDate](onDateUpdate)
    val TimeInput = new ReusableInput[MomentJSTime](onTimeUpdate)

    def render(props: Props, state: State) = {
      <.div(
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Date"),
          <.div(^.`class` := "col-sm-10",
            DateInput(state.date)
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Time"),
          <.div(^.`class` := "col-sm-10",
            TimeInput(state.time)
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("AtTriggerInput").
    initialState_P(_ => State(None, None)).
    renderBackend[Backend].
    build

  def apply(value: Option[Trigger.At], onUpdate: Option[Trigger.At] => Callback) =
    component(Props(value, onUpdate))

}
