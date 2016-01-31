package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.console.client.layout.InputField
import io.kairos.id.ArtifactId
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 23/12/2015.
  */
object JobForm {
  import MonocleReact._

  type RegisterHandler = JobSpec => Callback

  class JobFormBackend($: BackendScope[RegisterHandler, JobSpec]) {

    def submitJob(event: ReactEventI): Callback = {
      event.preventDefaultCB >>
        $.state.flatMap(spec => $.props.flatMap(handler => handler(spec)))
    }

  }

  private[this] val component = ReactComponentB[RegisterHandler]("JobForm").
    initialState(JobSpec(displayName = "", artifactId = ArtifactId("", "", ""), jobClass = "")).
    backend(new JobFormBackend(_)).
    render { $ =>
      val displayName = ExternalVar.state($.zoomL(JobSpec.displayName))
      lazy val description = {
        val readValue = (spec: JobSpec) =>
          JobSpec.description.get(spec).getOrElse("")
        val writeValue = (spec: JobSpec, value: String) =>
          JobSpec.description.set(Some(value))(spec)

        ExternalVar.state($.zoom(readValue)(writeValue))
      }
      val groupId     = ExternalVar.state($.zoomL(JobSpec.artifactId ^|-> ArtifactId.group))
      val artifactId  = ExternalVar.state($.zoomL(JobSpec.artifactId ^|-> ArtifactId.artifact))
      val version     = ExternalVar.state($.zoomL(JobSpec.artifactId ^|-> ArtifactId.version))
      val jobClass    = ExternalVar.state($.zoomL(JobSpec.jobClass))

      <.form(^.name := "jobDetails", ^.onSubmit ==> $.backend.submitJob,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "displayName", "Name"),
          InputField.text("displayName", "Job Name", required = true, displayName)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "description", "Description"),
          InputField.text("description", "Description", required = false, description)
        ),
        <.div(^.`class` := "form-group",
          <.label("Artifact ID"),
          <.div(^.`class` := "container-fluid",
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-md-4", InputField.text("group", "GroupId", required = true, groupId)),
              <.div(^.`class` := "col-md-4", InputField.text("name", "ArtifactId", required = true, artifactId)),
              <.div(^.`class` := "col-md-4", InputField.text("version", "Version", required = true, version))
            )
          )
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "jobClass", "Job Class"),
          InputField.text("jobClass", "Job class name", required = true, jobClass)
        ),
        <.button(^.`class` := "btn btn-default", "Submit")
      )
    }.
    build

  def apply(handler: RegisterHandler) = component(handler)

}
