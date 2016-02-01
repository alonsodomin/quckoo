package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.id.JobId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object JobSpecList {

  case class Props(specs: Map[JobId, JobSpec])

  private[this] val component = ReactComponentB[Props]("JobSpecList").
    stateless.
    noBackend.
    render_P(p =>
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th("Name"),
            <.th("Description"),
            <.th("ArtifactId"),
            <.th("Job Class")
          )
        ),
        <.tbody(
          p.specs.map { case (jobId, spec) =>
            <.tr(^.key := jobId.toString(),
              <.td(spec.displayName),
              <.td(spec.description),
              <.td(spec.artifactId.toString()),
              <.td(spec.jobClass)
            )
          }
        )
      )
    ).build

  def apply(specs: Map[JobId, JobSpec]) = component(Props(specs))

}
