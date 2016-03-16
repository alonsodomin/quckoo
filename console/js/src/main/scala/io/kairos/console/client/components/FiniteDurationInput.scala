package io.kairos.console.client.components

import java.util.concurrent.TimeUnit

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 31/01/2016.
  */
object FiniteDurationInput {

  val SupportedUnits = Seq(
    MILLISECONDS -> "Milliseconds",
    SECONDS      -> "Seconds",
    MINUTES      -> "Minutes",
    HOURS        -> "Hours",
    DAYS         -> "Days"
  )

  case class Props(id: String, value: FiniteDuration, onChange: FiniteDuration => Callback)

  case class State(current: FiniteDuration)

  class Backend($: BackendScope[Props, FiniteDuration]) {

    def notifyOnChange: Callback =
      $.props.map(_.onChange).flatMap { handler =>
        $.state.flatMap(handler)
      }

    def updateAmount(x: Long): Callback =
      $.modState(current => FiniteDuration(x, current.unit)) >> notifyOnChange

    def updateUnit(evt: ReactEventI): Callback =
      $.modState(current => FiniteDuration(current.length, TimeUnit.valueOf(evt.target.value))) >> notifyOnChange

    def render(props: Props, state: FiniteDuration) = {
      val id = props.id

      <.div(^.`class` := "container-fluid",
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-2",
            Input.long(state.length, updateAmount, ^.id := s"${id}_amount", ^.placeholder := "0")
          ),
          <.div(^.`class` := "col-sm-6",
            <.select(^.id := s"${id}_unit", ^.`class` := "form-control",
              ^.value := state.unit.toString(),
              ^.onChange ==> updateUnit,
              SupportedUnits.map { case (u, text) =>
                <.option(^.value := u.name(), text)
              }
            )
          )
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("AmountOfTime").
    initialState_P(_.value).
    renderBackend[Backend].
    build

  def apply(id: String, onChange: FiniteDuration => Callback) =
    component(Props(id, 0.seconds, onChange))

  def apply(id: String, value: FiniteDuration, onChange: FiniteDuration => Callback) =
    component(Props(id, value, onChange))

}
