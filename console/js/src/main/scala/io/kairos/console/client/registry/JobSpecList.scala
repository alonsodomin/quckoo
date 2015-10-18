package io.kairos.console.client.registry

import io.kairos.console.protocol.JobSpecDetails
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object JobSpecList {

  case class Props(specs: Seq[JobSpecDetails])

  private[this] val component = ReactComponentB[Props]("JobSpecList").
    stateless.
    noBackend.
    render((p, _, _) =>
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(<.th("Id"), <.th("Name"))
        ),
        <.tbody(
          p.specs.map(spec => {
            <.tr(
              <.td(spec.id), <.td(spec.name)
            )
          })
        )
      )
    ).build

  def apply(specs: Seq[JobSpecDetails]) = component(Props(specs))

}
