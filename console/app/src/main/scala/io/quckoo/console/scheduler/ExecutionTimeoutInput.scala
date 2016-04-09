package io.quckoo.console.scheduler

import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 09/04/2016.
  */
object ExecutionTimeoutInput {

  case class Props(value: Option[FiniteDuration], onUpdate: Option[FiniteDuration] => Callback)
  case class State(enabled: Boolean = false)

  class Backend($: BackendScope[Props, State]) {

    def onFlagUpdate: Callback =
      $.modState(st => st.copy(enabled = !st.enabled))

    def onValueUpdate(value: Option[FiniteDuration]): Callback =
      $.props.flatMap(_.onUpdate(value))

    def render(props: Props, state: State) = {
      <.div(
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Timeout"),
          <.div(^.`class` := "col-sm-10",
            <.div(^.`class` := "checkbox",
              <.label(
                <.input.checkbox(
                  ^.id := "enableTimeout",
                  ^.value := state.enabled,
                  ^.onChange --> onFlagUpdate
                ),
                "Enabled"
              )
            )
          )
        ),
        if (state.enabled) {
          <.div(^.`class` := "col-sm-offset-2",
            FiniteDurationInput("timeout", props.value, onValueUpdate)
          )
        } else EmptyTag
      )
    }

  }

  val component = ReactComponentB[Props]("ExecutionTimeout").
    initialState(State()).
    renderBackend[Backend].
    build

  def apply(value: Option[FiniteDuration], onUpdate: Option[FiniteDuration] => Callback) =
    component(Props(value, onUpdate))

}
