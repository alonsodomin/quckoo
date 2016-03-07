package io.kairos.console.client.registry

import diode.react.ModelProxy
import io.kairos._
import io.kairos.console.client.components.{Button, Icons}
import io.kairos.console.client.core.{RegisterJob, RegistryModel}
import io.kairos.console.client.layout.{Notification, NotificationDisplay}
import io.kairos.id.JobId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object RegistryPage {

  object Style extends StyleSheet.Inline {

    val content = style()
  }

  case class Props(proxy: ModelProxy[RegistryModel])
  case class State(
      notifications: Seq[Notification] = Seq(),
      specs: Map[JobId, JobSpec] = Map.empty,
      selectedJob: Option[JobSpec] = None,
      showForm: Boolean = false
  )

  class RegistryBackend($: BackendScope[Props, State]) {

    def editJob(spec: Option[JobSpec]) =
      $.modState(_.copy(selectedJob = spec, showForm = true))

    def jobEdited(spec: JobSpec): Callback = {
      def dispatchAction(props: Props): Callback =
        props.proxy.dispatch(RegisterJob(spec))

      def updateState: Callback =
        $.modState(_.copy(showForm = false))

      Callback.log("Registering job...") >>
        ($.props >>= dispatchAction) >>
        updateState
    }

    def render(props: Props, state: State) =
      <.div(Style.content,
        <.h2("Registry"),
        props.proxy().lastErrors.map { errors =>
          NotificationDisplay(Seq(
            Notification.danger {
              <.div(
                <.p("Could not register the job due to following errors:"),
                errors.list.toList.map {
                  case UnresolvedDependency(artifactId) =>
                    <.li(s"Unresolved dependency: $artifactId")
                  case error: Fault => <.li(error.toString())
                }
              )
            }
          ))
        },
        NotificationDisplay(state.notifications),
        Button(Button.Props(Some(editJob(None))), Icons.plusSquare, "Register"),
        if (state.showForm) JobForm(state.selectedJob, jobEdited)
        else EmptyTag,
        props.proxy.wrap(_.jobSpecs)(JobSpecList(_))
      )

  }

  private[this] val component = ReactComponentB[Props]("RegistryPage").
    initialState(State()).
    renderBackend[RegistryBackend].
    build

  def apply(proxy: ModelProxy[RegistryModel]) = component(Props(proxy))

}
