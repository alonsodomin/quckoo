package io.kairos.console.client.time

import io.kairos.console.client.layout.InputField
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 31/01/2016.
  */
object AmountOfTimeField {
  import MonocleReact._

  type Props = (String, Boolean, ExternalVar[AmountOfTime])

  val SupportedUnits = Seq(
    SECONDS -> "Seconds",
    MINUTES -> "Minutes",
    HOURS   -> "Hours",
    DAYS    -> "Days"
  )

  private[this] val component = ReactComponentB[Props]("FrequencyField").
    initialState_P(_._3).
    render { $ =>
      val (id, required, freq) = $.props
      //val updateAmount = (event: ReactEventI) => freq.mod(_.copy(amount = event.target.value.toInt))
      val amount = ExternalVar(freq.value.amount)(f => freq.setL(AmountOfTime.amount)(f))

      <.div(^.`class` := "container-fluid",
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-2",
            InputField.int(s"${id}_amount", "0", required, amount)
          ),
          <.div(^.`class` := "col-sm-2",
            <.select(^.id := s"${id}_unit", ^.`class` := "form-control", ^.required := required,
              SupportedUnits.map { case (unit, text) =>
                <.option(^.value := unit.name(), text)
              }
            )
          )
        )
      )
    } build

  def apply(id: String, required: Boolean, field: ExternalVar[AmountOfTime]) =
    component((id, required, field))

}
