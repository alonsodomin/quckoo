package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.id.ArtifactId
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 23/12/2015.
  */
object JobForm {

  type RegisterHandler = JobSpec => Callback

  object ArtifactField {

    private[this] val component = ReactComponentB[(String, String, ExternalVar[String])]("Artifact Field").
      render_P { case (id, placeholder, field) =>
        val updateField = (event: ReactEventI) => field.set(event.target.value)
        <.input.text(^.id := id,
          ^.`class` := "form-control",
          ^.required := true,
          ^.placeholder := placeholder,
          ^.value := field.value,
          ^.onChange ==> updateField)
      } build

    def apply(fieldId: String, placeholder: String, fieldVar: ExternalVar[String]) =
      component((fieldId, placeholder, fieldVar))

  }

  object ArtifactDetails {
    import MonocleReact._

    private[this] val component = ReactComponentB[JobSpec]("Artifact Details").
      initialState_P(_.artifactId).
      render { $ =>
        val group = ExternalVar.state($.zoomL(ArtifactId.group))
        val artifactName = ExternalVar.state($.zoomL(ArtifactId.artifact))
        val version = ExternalVar.state($.zoomL(ArtifactId.version))
        <.div(
          ArtifactField("group", "GroupId", group),
          ArtifactField("name", "ArtifactId", artifactName),
          ArtifactField("version", "Version", version)
        )
      } build

    def apply(spec: JobSpec) = component(spec)

  }

  class JobFormBackend($: BackendScope[RegisterHandler, JobSpec]) {

    def updateName(event: ReactEventI): Callback =
      $.modState(_.copy(displayName = event.target.value))

    def updateDescription(event: ReactEventI): Callback =
      $.modState(_.copy(description = event.target.value))

    def updateClassName(event: ReactEventI): Callback =
      $.modState(_.copy(jobClass = event.target.value))

    def submitJob(event: ReactEventI): Callback = {
      event.preventDefaultCB >>
        $.state.flatMap(spec => $.props.flatMap(handler => handler(spec)))
    }

    def render(spec: JobSpec) = {
      <.form(^.name := "jobDetails", ^.onSubmit ==> submitJob,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "name", "Name"),
          <.input.text(^.id := "name", ^.`class` := "form-control", ^.placeholder := "Job Name",
            ^.required := true, ^.onChange ==> updateName, ^.value := spec.displayName)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "description", "Description"),
          <.input.text(^.id := "description", ^.`class` := "form-control", ^.placeholder := "Job Description",
            ^.required := false, ^.onChange ==> updateDescription, ^.value := spec.description)
        ),
        <.div(^.`class` := "form-group",
          <.label("Artifact ID"),
          ArtifactDetails(spec)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "jobClass", "Job Class"),
          <.input.text(^.id := "jobClass", ^.`class` := "form-control", ^.placeholder := "Job class name",
            ^.required := true, ^.onChange ==> updateClassName, ^.value := spec.jobClass
          )
        ),
        <.button(^.`class` := "btn btn-default", "Submit")
      )
    }

  }

  private[this] val component = ReactComponentB[RegisterHandler]("JobForm").
    initialState(JobSpec(displayName = "", artifactId = ArtifactId("", "", ""), jobClass = "")).
    renderBackend[JobFormBackend].
    build

  def apply(handler: RegisterHandler) = component(handler)

}
