package io.quckoo.console.registry

import io.quckoo.JobSpec
import io.quckoo.id.JobId

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 09/04/2016.
  */
object JobSelect {

  case class Props(jobs: Map[JobId, JobSpec], value: Option[JobId], onUpdate: Option[JobId] => Callback)

  class Backend($: BackendScope[Props, Unit]) {

    def onUpdate(evt: ReactEventI): Callback = {
      val newValue = {
        if (evt.target.value.isEmpty) None
        else Some(JobId(evt.target.value))
      }

      $.props.flatMap(_.onUpdate(newValue))
    }

    def render(props: Props) = {
      <.div(^.`class` := "form-group",
        <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "jobId", "Job"),
        <.div(^.`class` := "col-sm-10",
          <.select(^.id := "jobId", ^.`class` := "form-control",
            props.value.map(id => ^.value := id.toString()),
            ^.onChange ==> onUpdate,
            <.option("Select a job"),
            props.jobs.map { case (jobId, spec) =>
              val desc = spec.description.map(text=> s"| $text").getOrElse("")
              <.option(^.value := jobId.toString(),
                s"${spec.displayName} $desc"
              )
            }
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("JobSelect").
    stateless.
    renderBackend[Backend].
    build

  def apply(jobs: Map[JobId, JobSpec], value: Option[JobId], onUpdate: Option[JobId] => Callback) =
    component(Props(jobs, value, onUpdate))

}
