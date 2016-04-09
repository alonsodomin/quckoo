package io.quckoo.console.scheduler

import io.quckoo.Trigger
import io.quckoo.console.components._
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 08/04/2016.
  */
object EveryTriggerInput {

  case class Props(value: Option[Trigger.Every], onUpdate: Option[Trigger.Every] => Callback)
  case class State(freq: Option[FiniteDuration], delay: Option[FiniteDuration]) {

    def this(trigger: Option[Trigger.Every]) =
      this(trigger.map(_.frequency), trigger.flatMap(_.startingIn))

  }

  implicit val propsReuse = Reusability.by[Props, Option[Trigger.Every]](_.value)
  implicit val stateReuse = Reusability.caseClass[State]

  class Backend($: BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val value = $.state.map(st => st.freq.map(freq => (freq, st.delay)))
      value.flatMap {
        case Some((freq, delay)) =>
          val trigger = Trigger.Every(freq, delay)
          $.props.flatMap(_.onUpdate(Some(trigger)))

        case _ =>
          $.props.flatMap(_.onUpdate(None))
      }
    }

    def onFreqUpdate(value: Option[FiniteDuration]): Callback =
      $.modState(_.copy(freq = value), propagateUpdate)

    def onDelayUpdate(value: Option[FiniteDuration]): Callback =
      $.modState(_.copy(delay = value), propagateUpdate)

    def render(props: Props, state: State) = {
      <.div(
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Frequency"),
          <.div(^.`class` := "col-sm-10",
            FiniteDurationInput("everyTrigger_freq", state.freq, onFreqUpdate)
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Delay"),
          <.div(^.`class` := "col-sm-10",
            FiniteDurationInput("everyTrigger_delay", state.delay, onDelayUpdate)
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("EveryTriggerInput").
    initialState_P(props => new State(props.value)).
    renderBackend[Backend].
    configure(Reusability.shouldComponentUpdate).
    build

  def apply(value: Option[Trigger.Every], onUpdate: Option[Trigger.Every] => Callback) =
    component(Props(value, onUpdate))

}
