package io.kairos.console.client.registry

import diode.data.PotMap
import diode.react.ModelProxy
import diode.react.ReactPot._
import io.kairos.JobSpec
import io.kairos.console.client.core.LoadJobSpecs
import io.kairos.fault._
import io.kairos.id.JobId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object JobSpecList {

  case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props) = {
      def dispatchJobLoading: Callback =
        Callback.log("Loading job specs...") >> props.proxy.dispatch(LoadJobSpecs)

      Callback.ifTrue(props.proxy().size == 0, dispatchJobLoading)
    }

    def render(p: Props) = {
      val model = p.proxy()
      <.table(^.`class` := "table table-striped",
        <.thead(
          <.tr(
            <.th("Name"),
            <.th("Description"),
            <.th("ArtifactId"),
            <.th("Job Class")
          )
        ),
        <.tbody(
          model.seq.map { case (jobId, spec) =>
            <.tr(^.key := jobId.toString(),
              spec.renderFailed { ex =>
                ex.printStackTrace()
                <.td(^.colSpan := 4, ExceptionThrown(ex).toString())
              },
              spec.renderPending(_ > 500, _ => "Loading ..."),
              spec.render { item => List(
                <.td(item.displayName),
                <.td(item.description),
                <.td(item.artifactId.toString()),
                <.td(item.jobClass)
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
