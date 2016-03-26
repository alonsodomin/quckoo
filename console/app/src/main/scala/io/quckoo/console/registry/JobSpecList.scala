package io.quckoo.console.registry

import diode.data.PotMap
import diode.react.ModelProxy
import diode.react.ReactPot._

import io.quckoo.JobSpec
import io.quckoo.console.components._
import io.quckoo.console.core.LoadJobSpecs
import io.quckoo.fault._
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object JobSpecList {

  case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]])
  case class State(selected: Set[JobId], allSelected: Boolean = false)

  class Backend($: BackendScope[Props, State]) {

    def mounted(props: Props) = {
      def dispatchJobLoading: Callback =
        Callback.log("Loading job specs...") >> props.proxy.dispatch(LoadJobSpecs)

      Callback.ifTrue(props.proxy().size == 0, dispatchJobLoading)
    }

    def toggleSelectAll(props: Props): Callback =
      $.modState { state =>
        if (state.allSelected) state.copy(selected = Set.empty, allSelected = false)
        else state.copy(selected = props.proxy().keys.toSet, allSelected = true)
      }

    def toggleSelected(props: Props, jobId: JobId): Callback = {
      $.modState { state =>
        val newSet = {
          if (state.selected.contains(jobId))
            state.selected - jobId
          else state.selected + jobId
        }
        state.copy(selected = newSet, allSelected = newSet.size == props.proxy().size)
      }
    }

    def enableJob(props: Props, jobId: JobId): Callback =
      props.proxy.dispatch(EnableJob(jobId))

    def disableJob(props: Props, jobId: JobId): Callback =
      props.proxy.dispatch(DisableJob(jobId))

    def render(p: Props, state: State) = {
      val model = p.proxy()
      <.table(^.`class` := "table table-striped table-hover",
        <.thead(
          <.tr(
            <.th(<.input.checkbox(
              ^.id := "selectAllJobs",
              ^.value := state.allSelected,
              ^.onChange --> toggleSelectAll(p)
            )),
            <.th("Name"),
            <.th("Description"),
            <.th("ArtifactId"),
            <.th("Job Class"),
            <.th("Status"),
            <.th("Actions")
          )
        ),
        <.tbody(
          model.seq.map { case (jobId, spec) => List(
            spec.renderFailed { ex =>
              <.tr(<.td(^.colSpan := 7, Notification.danger(ExceptionThrown(ex))))
            },
            spec.renderPending(_ > 500, _ =>
              <.tr(<.td(^.colSpan := 7, "Loading ..."))
            ),
            spec.render { item => <.tr(
              state.selected.contains(jobId) ?= (^.`class` := "info"),
              <.td(<.input.checkbox(
                ^.id := s"selectJob_$jobId",
                ^.value := state.selected.contains(jobId),
                ^.onChange --> toggleSelected(p, jobId)
              )),
              <.td(item.displayName),
              <.td(item.description),
              <.td(item.artifactId.toString()),
              <.td(item.jobClass),
              <.td(
                if (item.disabled) {
                  <.span(^.color.red, "DISABLED")
                } else {
                  <.span(^.color.green, "ENABLED")
                }
              ),
              <.td(
                if (item.disabled) {
                  Button(Button.Props(Some(enableJob(p, jobId))), Icons.play, "Enable")
                } else {
                  Button(Button.Props(Some(disableJob(p, jobId))), Icons.stop, "Disable")
                }
              )
            )}
          )}
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("JobSpecList").
    initialState(State(Set.empty)).
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]]) = component(Props(proxy))

}
