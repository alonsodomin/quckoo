package io.kairos.console.client.registry

import io.kairos.JobSpec
import io.kairos.console.client.core.ClientApi
import io.kairos.id.JobId
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object RegistryPage {

  object Style extends StyleSheet.Inline {

    val content = style()
  }

  case class State(specs: Map[JobId, JobSpec] = Map.empty)

  private[this] val component = ReactComponentB[Unit]("RegistryPage").
    initialState(State()).
    noBackend.
    render_S(s => {
      <.div(Style.content,
        <.h2("Registry"),
        JobForm(),
        JobSpecList(s.specs)
      )
    }).
    componentDidMount($ => Callback.future {
      ClientApi.getJobs() map { case specMap: Map[JobId, JobSpec] =>
        $.modState(_.copy(specs = specMap))
      }
    }).buildU

  def apply() = component()

}
