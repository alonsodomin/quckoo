package io.kairos.console.client.registry

import diode.data.PotMap
import diode.react.ModelProxy
import diode.react.ReactPot._

import io.kairos.JobSpec
import io.kairos.console.client.components._
import io.kairos.console.client.core.LoadJobSpecs
import io.kairos.fault._
import io.kairos.id.JobId
import io.kairos.protocol.RegistryProtocol

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object JobSpecList {
  import RegistryProtocol._

  case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props) = {
      def dispatchJobLoading: Callback =
        Callback.log("Loading job specs...") >> props.proxy.dispatch(LoadJobSpecs)

      Callback.ifTrue(props.proxy().size == 0, dispatchJobLoading)
    }

    def enableJob(props: Props, jobId: JobId): Callback =
      props.proxy.dispatch(EnableJob(jobId))

    def disableJob(props: Props, jobId: JobId): Callback =
      props.proxy.dispatch(DisableJob(jobId))

    def render(p: Props) = {
      val model = p.proxy()
      <.table(^.`class` := "table table-striped table-hover",
        <.thead(
          <.tr(
            <.th("Name"),
            <.th("Description"),
            <.th("ArtifactId"),
            <.th("Job Class"),
            <.th("Status"),
            <.th("Actions")
          )
        ),
        <.tbody(
          model.seq.map { case (jobId, spec) =>
            <.tr(^.key := jobId.toString(),
              spec.renderFailed { ex =>
                ex.printStackTrace()
                <.td(^.colSpan := 6, Notification.danger(ExceptionThrown(ex)))
              },
              spec.renderPending(_ > 500, _ => "Loading ..."),
              spec.render { item => List(
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
                    Button(Button.Props(Some(enableJob(p, jobId))), Icons.playCircleO, "Enable")
                  } else {
                    Button(Button.Props(Some(disableJob(p, jobId))), Icons.stopCircleO, "Disable")
                  }
                )
              )}
            )
          }
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("JobSpecList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]]) = component(Props(proxy))

}
