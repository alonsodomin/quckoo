package io.kairos.console.client.components

import io.kairos.console.client.time.AmountOfTime
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 31/01/2016.
  */
object AmountOfTimeInput {
  import MonocleReact._

  case class Props(id: String, field: ExternalVar[AmountOfTime])

  val SupportedUnits = Seq(
    SECONDS -> "Seconds",
    MINUTES -> "Minutes",
    HOURS   -> "Hours",
    DAYS    -> "Days"
  )

  private[this] val component = ReactComponentB[Props]("AmountOfTime").
    initialState_P(_.field).
    render { $ =>
      val id = $.props.id
      val freq = $.props.field
      val amount = ExternalVar(freq.value.amount)(f => freq.setL(AmountOfTime.amount)(f))

      <.div(^.`class` := "container-fluid",
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-2",
            Input.int(amount.value, amount.set, ^.id := s"${id}_amount", ^.placeholder := "0")
          ),
          <.div(^.`class` := "col-sm-2",
            <.select(^.id := s"${id}_unit", ^.`class` := "form-control",
              SupportedUnits.map { case (unit, text) =>
                <.option(^.value := unit.name(), text)
              }
            )
          )
        )
      )
    } build

  def apply(id: String, field: ExternalVar[AmountOfTime]) =
    component(Props(id, field))

}
