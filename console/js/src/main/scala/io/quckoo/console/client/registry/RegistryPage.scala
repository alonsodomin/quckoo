package io.quckoo.console.client.registry

import diode.react.ModelProxy

import io.quckoo._
import io.quckoo.console.client.components._
import io.quckoo.console.client.core.ConsoleModel
import io.quckoo.protocol.RegistryProtocol

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object RegistryPage {
  import RegistryProtocol._

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))

  }

  case class Props(proxy: ModelProxy[ConsoleModel])
  case class State(
      selectedJob: Option[JobSpec] = None,
      showForm: Boolean = false
  )

  class RegistryBackend($: BackendScope[Props, State]) {

    def editJob(spec: Option[JobSpec]) =
      $.modState(_.copy(selectedJob = spec, showForm = true))

    def jobEdited(spec: JobSpec): Callback = {
      def dispatchAction(props: Props): Callback =
        props.proxy.dispatch(RegisterJob(spec))

      def updateState(): Callback =
        $.modState(_.copy(showForm = false))

      Callback.log("Registering job...") >>
        ($.props >>= dispatchAction) >>
        updateState()
    }

    def render(props: Props, state: State) =
      <.div(Style.content,
        <.h2("Registry"),
        props.proxy().notification,
        Button(Button.Props(Some(editJob(None))), Icons.plusSquare, "New Job"),
        if (state.showForm) JobForm(state.selectedJob, jobEdited)
        else EmptyTag,
        props.proxy.wrap(_.jobSpecs)(JobSpecList(_))
      )

  }

  private[this] val component = ReactComponentB[Props]("RegistryPage").
    initialState(State()).
    renderBackend[RegistryBackend].
    build

  def apply(proxy: ModelProxy[ConsoleModel]) = component(Props(proxy))

}
