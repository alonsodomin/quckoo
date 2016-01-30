package io.kairos.console.client.execution

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 30/01/2016.
  */
object ExecutionPlanList {

  case class Props(plans: Seq[String])

  private[this] val component = ReactComponentB[Props]("ExecutionPlanList").
    stateless.
    noBackend.
    render_P { p =>
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(<.th("Plan ID"))
        ),
        <.tbody(
          p.plans map { plan =>
            <.tr(
              <.td(plan)
            )
          }
        )
      )
    } build

  def apply(plans: Seq[String]) = component(Props(plans))

}
