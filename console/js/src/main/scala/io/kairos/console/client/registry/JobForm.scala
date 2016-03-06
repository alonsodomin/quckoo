package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.console.client.components._
import io.kairos.console.client.layout.FormField
import io.kairos.id.ArtifactId
import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.ExternalVar
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros.Lenses
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 23/12/2015.
  */
object JobForm {
  import MonocleReact._

  type RegisterHandler = JobSpec => Callback

  case class Props(spec: Option[JobSpec], handler: RegisterHandler)

  @Lenses
  case class State(spec: JobSpec, cancelled: Boolean = true)

  class JobFormBackend($: BackendScope[Props, State]) {

    def submitJob(event: ReactEventI): Callback = {
      event.preventDefaultCB >>
        $.state.map(_.spec).flatMap(spec => $.props.map(_.handler).flatMap(handler => handler(spec)))
    }

    def render(props: Props, state: State) = {
      //val displayName = ExternalVar.state($.accessCB.zoomL(State.spec ^|-> JobSpec.displayName))
      /*lazy val description = {
        val readValue = (spec: JobSpec) =>
          JobSpec.description.get(spec).getOrElse("")
        val writeValue = (spec: JobSpec, value: String) =>
          JobSpec.description.set(Some(value))(spec)

        ExternalVar.state($.zoom(readValue)(writeValue))
      }*/
      /*val groupId     = ExternalVar.state($.zoomL(State.spec ^|-> JobSpec.artifactId ^|-> ArtifactId.group))
      val artifactId  = ExternalVar.state($.zoomL(State.spec ^|-> JobSpec.artifactId ^|-> ArtifactId.artifact))
      val version     = ExternalVar.state($.zoomL(State.spec ^|-> JobSpec.artifactId ^|-> ArtifactId.version))
      val jobClass    = ExternalVar.state($.zoomL(State.spec ^|-> JobSpec.jobClass))*/

      <.form(^.name := "jobDetails", ^.`class` := "form-horizontal", ^.onSubmit ==> submitJob,
        Modal(Modal.Props(
          header = hide => <.span(<.button(^.tpe := "button", lookAndFeel.close, ^.onClick --> hide, Icons.close), <.h4("Register Job")),
          footer = hide => <.span(Button(Button.Props(style = ContextStyle.primary), "Ok")),
          closed = Callback.empty),
          <.span("Hello!")
        )
        /*Modal(Modal.Props(
          header = hide => <.span(<.button(^.tpe := "button", lookAndFeel.close, ^.onClick --> hide, Icons.close), <.h4("Register Job")),
          footer = hide => <.span(Button(Button.Props(style = ContextStyle.primary), "Ok")),
          closed = Callback.empty),
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
                    )
                  ),
                  <.div(^.`class` := "col-sm-4",
                    FormField.text("name",
                      placeholder = Some("Artifact Id"),
                      validator = FormField.notEmptyStr("artifactId"),
                      accessor = artifactId
                    )
                  ),
                  <.div(^.`class` := "col-sm-4",
                    FormField.text("version",
                      placeholder = Some("Version"),
                      validator = FormField.notEmptyStr("version"),
                      accessor = version
                    )
                  )
                )
              )
            )
          ),
          FormField.text("jobClass",
            label = Some("Job Class"),
            placeholder = Some("Fully qualified job class name"),
            validator = FormField.notEmptyStr("jobClass"),
            accessor = jobClass
          )
        )*/
      )
    }

  }

  private[this] def emptyJobSpec = JobSpec(displayName = "", artifactId = ArtifactId("", "", ""), jobClass = "")

  private[this] val component = ReactComponentB[Props]("JobForm").
    initialState_P(p => State(p.spec.getOrElse(emptyJobSpec))).
    renderBackend[JobFormBackend].
    build

  def apply(spec: Option[JobSpec], handler: RegisterHandler) = component(Props(spec, handler))

}
