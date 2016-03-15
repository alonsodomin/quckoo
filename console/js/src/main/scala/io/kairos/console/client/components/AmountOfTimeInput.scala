package io.kairos.console.client.components

import java.util.concurrent.TimeUnit

import io.kairos.console.client.time.AmountOfTime
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros.Lenses

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 31/01/2016.
  */
object AmountOfTimeInput {
  import MonocleReact._

  val SupportedUnits = Seq(
    SECONDS -> "Seconds",
    MINUTES -> "Minutes",
    HOURS   -> "Hours",
    DAYS    -> "Days"
  )

  case class Props(id: String, value: Option[AmountOfTime], onChange: AmountOfTime => Callback)

  @Lenses
  case class State(current: AmountOfTime)

  class Backend($: BackendScope[Props, AmountOfTime]) {

    def notifyOnChange: Callback =
      $.props.map(_.onChange).flatMap { handler =>
        $.state.flatMap(handler)
      }

    def updateAmount(x: Int): Callback =
      $.setStateL(AmountOfTime.amount)(x) >> notifyOnChange

    def updateUnit(evt: ReactEventI): Callback =
      $.setStateL(AmountOfTime.unit)(TimeUnit.valueOf(evt.target.value)) >> notifyOnChange

    def render(props: Props, state: AmountOfTime) = {
      val id = props.id

      <.div(^.`class` := "container-fluid",
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-2",
            Input.int(state.amount, updateAmount, ^.id := s"${id}_amount", ^.placeholder := "0")
          ),
          <.div(^.`class` := "col-sm-2",
            <.select(^.id := s"${id}_unit", ^.`class` := "form-control",
              ^.onChange ==> updateUnit,
              SupportedUnits.map { case (u, text) =>
                <.option(^.selected := (u == state.unit), ^.value := u.name(), text)
              }
            )
          )
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("AmountOfTime").
    initialState_P(_.value.getOrElse(AmountOfTime())).
    renderBackend[Backend].
    build

  def apply(id: String, value: Option[AmountOfTime], onChange: AmountOfTime => Callback) =
    component(Props(id, value, onChange))

}
