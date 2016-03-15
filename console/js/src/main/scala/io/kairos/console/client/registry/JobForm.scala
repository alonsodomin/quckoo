package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.console.client.components._
import io.kairos.id.ArtifactId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monocle.macros.Lenses
import monocle.std.option._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 23/12/2015.
  */
object JobForm {
  import MonocleReact._

  @inline
  private def lnf = lookAndFeel

  type RegisterHandler = JobSpec => Callback

  case class Props(spec: Option[JobSpec], handler: RegisterHandler)

  @Lenses
  case class State(spec: JobSpec, cancelled: Boolean = true)

  class JobFormBackend($: BackendScope[Props, State]) {

    val displayName     = State.spec ^|-> JobSpec.displayName
    val description     = State.spec ^|-> JobSpec.description ^<-? some
    val artifactGroup   = State.spec ^|-> JobSpec.artifactId ^|-> ArtifactId.group
    val artifactName    = State.spec ^|-> JobSpec.artifactId ^|-> ArtifactId.artifact
    val artifactVersion = State.spec ^|-> JobSpec.artifactId ^|-> ArtifactId.version
    val jobClass        = State.spec ^|-> JobSpec.jobClass

    def updateDisplayName(evt: ReactEventI) =
      $.setStateL(displayName)(evt.target.value)

    def updateDescription(evt: ReactEventI) =
      $.setStateL(description)(evt.target.value)

    def updateArtifactName(evt: ReactEventI) =
      $.setStateL(artifactName)(evt.target.value)

    def updateArtifactGroup(evt: ReactEventI) =
      $.setStateL(artifactGroup)(evt.target.value)

    def updateArtifactVersion(evt: ReactEventI) =
      $.setStateL(artifactVersion)(evt.target.value)

    def updateJobClass(evt: ReactEventI) =
      $.setStateL(jobClass)(evt.target.value)

    def submitForm(): Callback =
      $.modState(_.copy(cancelled = false))

    def formClosed(props: Props, state: State) =
      if (state.cancelled) Callback.empty
      else props.handler(state.spec)

    def render(props: Props, state: State) = {
      <.form(^.name := "jobDetails",
        Modal(
          Modal.Props(
            header = hide => <.span(
              <.button(^.tpe := "button", lookAndFeel.close, ^.onClick --> hide, Icons.close),
              <.h4("Register Job")
            ),
            footer = hide => <.span(
              Button(Button.Props(Some(hide), style = ContextStyle.default), "Cancel"),
              Button(Button.Props(Some(submitForm() >> hide), style = ContextStyle.primary), "Ok")
            ),
            closed = formClosed(props, state)
          ),
          <.div(lnf.formGroup,
            <.label(^.`for` := "displayName", "Display Name"),
            <.input.text(lnf.formControl, ^.id := "displayName",
              ^.placeholder := "Job's name",
              ^.onChange ==> updateDisplayName
            )
          ),
          <.div(lnf.formGroup,
            <.label(^.`for` := "description", "Description"),
            <.input.text(lnf.formControl, ^.id := "description",
              ^.placeholder := "Job's description",
              ^.onChange ==> updateDescription
            )
          ),
          <.div(lnf.formGroup,
            <.label("Artifact"),
            <.div(^.`class` := "row",
              <.div(^.`class` := "col-sm-4",
                <.input.text(lnf.formControl, ^.id := "artifactGroup",
                  ^.placeholder := "Group",
                  ^.onChange ==> updateArtifactGroup
                )
              ),
              <.div(^.`class` := "col-sm-4",
                <.input.text(lnf.formControl, ^.id := "artifactName",
                  ^.placeholder := "Name",
                  ^.onChange ==> updateArtifactName
                )
              ),
              <.div(^.`class` := "col-sm-4",
                <.input.text(lnf.formControl, ^.id := "artifactVersion",
                  ^.placeholder := "Version",
                  ^.onChange ==> updateArtifactVersion
                )
              )
            )
          ),
          <.div(lnf.formGroup,
            <.label(^.`for` := "jobClass", "Job Class"),
            <.input.text(lnf.formControl, ^.id := "jobClass",
              ^.placeholder := "Fully classified job class name",
              ^.onChange ==> updateJobClass
            )
          )
        )
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
