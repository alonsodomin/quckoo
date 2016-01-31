package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.console.client.layout.{Button, FormField}
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

      <.form(^.name := "jobDetails", ^.`class` := "form-horizontal", ^.onSubmit ==> $.backend.submitJob,
        FormField.text("displayName",
          label = Some("Name"),
          placeholder = Some("Job name"),
          validator = FormField.notEmptyStr("displayName"),
          accessor = displayName
        ),
        FormField.text("description",
          label = Some("Description"),
          placeholder = Some("Long description for the job"),
          accessor = description
        ),
        <.div(^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Artifact ID"),
          <.div(^.`class` := "col-sm-10",
            <.div(^.`class` := "container-fluid",
              <.div(^.`class` := "row",
                <.div(^.`class` := "col-sm-4",
                  FormField.text("group",
                    placeholder = Some("Group Id"),
                    validator = FormField.notEmptyStr("groupId"),
                    accessor = groupId
                  )),
                <.div(^.`class` := "col-sm-4",
                  FormField.text("name",
                    placeholder = Some("Artifact Id"),
                    validator = FormField.notEmptyStr("artifactId"),
                    accessor = artifactId
                  )),
                <.div(^.`class` := "col-sm-4",
                  FormField.text("version",
                    placeholder = Some("Version"),
                    validator = FormField.notEmptyStr("version"),
                    accessor = version
                  ))
              )
            )
          )
        ),
        FormField.text("jobClass",
          label = Some("Job Class"),
          placeholder = Some("Fully qualified job class name"),
          validator = FormField.notEmptyStr("jobClass"),
          accessor = jobClass
        ),
        Button.submit()
      )
    }.
    build

  def apply(handler: RegisterHandler) = component(handler)

}
