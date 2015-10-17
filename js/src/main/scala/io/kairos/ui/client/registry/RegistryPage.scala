package io.kairos.ui.client.registry

import io.kairos.ui.client.core.ClientApi
import io.kairos.ui.protocol.JobSpecDetails
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Created by alonsodomin on 17/10/2015.
 */
object RegistryPage {

  case class State(specs: Seq[JobSpecDetails] = Seq())

  private[this] val component = ReactComponentB[Unit]("RegistryPage").
    initialState(State()).
    noBackend.
    render((_, s, _) => {
      <.div(JobSpecList(s.specs))
    }).
    componentDidMount($ => {
      ClientApi.getJobs() onSuccess { case jobDetails: Seq[JobSpecDetails] =>
        $.modState(_.copy(specs = jobDetails))
      }
    }).buildU

  def apply() = component()

}
