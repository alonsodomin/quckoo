package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.id.ModuleId
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 23/12/2015.
  */
object JobForm {

  type RegisterHandler = JobSpec => Callback

  case class JobDetails(name: String = "", description: String = "",
                        moduleId: ModuleId = ModuleId("", "", ""),
                        jobClass: String = "")

  object ModuleField {

    private[this] val component = ReactComponentB[(String, String, ExternalVar[String])]("Module Field").
      render_P { case (id, placeholder, field) =>
        val updateField = (event: ReactEventI) => field.set(event.target.value)
        <.input.text(^.id := id,
          ^.`class` := "form-control",
          ^.required := true,
          ^.placeholder := placeholder,
          ^.value := field.value,
          ^.onChange ==> updateField)
      } build

    def apply(fieldId: String, placeholder: String, fieldVar: ExternalVar[String]) = component((fieldId, placeholder, fieldVar))

  }

  object ModuleDetails {

    private[this] val component = ReactComponentB[ModuleId]("Module Details").
      render_P { p =>
        val group = ExternalVar(p.group)(g => Callback { p.copy(group = g) })
        val artifact = ExternalVar(p.artifact)(a => Callback { p.copy(artifact = a) })
        val version = ExternalVar(p.version)(v => Callback { p.copy(version = v) })
        <.div(
          ModuleField("group", "Group ID", group),
          ModuleField("artifact", "Artifact ID", artifact),
          ModuleField("version", "Version", version)
        )
      } build

    def apply(moduleId: ModuleId) = component(moduleId)

  }

  class JobFormBackend($: BackendScope[RegisterHandler, JobDetails]) {

    def updateName(event: ReactEventI): Callback =
      $.modState(_.copy(name = event.target.value))

    def updateDescription(event: ReactEventI): Callback =
      $.modState(_.copy(name = event.target.value))

    def updateClassName(event: ReactEventI): Callback =
      $.modState(_.copy(jobClass = event.target.value))

    def submitJob(event: ReactEventI): Callback = {
      preventDefault(event) >>
        $.state.map(details => JobSpec(details.name, details.description, details.moduleId, details.jobClass)).
          flatMap(spec => $.props.flatMap(handler => handler(spec)))
    }

    def render(details: JobDetails) = {
      <.form(^.name := "jobDetails", ^.onSubmit ==> submitJob,
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "name", "Name"),
          <.input.text(^.id := "name", ^.`class` := "form-control", ^.placeholder := "Job Name",
            ^.required := true, ^.onChange ==> updateName, ^.value := details.name)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "description", "Description"),
          <.input.text(^.id := "description", ^.`class` := "form-control", ^.placeholder := "Job Description",
            ^.required := false, ^.onChange ==> updateDescription, ^.value := details.description)
        ),
        <.div(^.`class` := "form-group",
          <.label("Module ID"),
          ModuleDetails(details.moduleId)
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`for` := "jobClass", "Job Class"),
          <.input.text(^.id := "jobClass", ^.`class` := "form-control", ^.placeholder := "Job class name",
            ^.required := true, ^.onChange ==> updateClassName, ^.value := details.jobClass
          )
        ),
        <.button(^.`class` := "btn btn-default", "Submit")
      )
    }

  }

  private[this] val component = ReactComponentB[RegisterHandler]("JobForm").
    initialState(JobDetails()).
    renderBackend[JobFormBackend].
    build

  def apply(handler: RegisterHandler) = component(handler)

}