package io.kairos.console.client

import io.kairos.console.client.core.{ClusterEventListener, ClusterEvent, ClientApi}
import io.kairos.console.protocol.ClusterDetails
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 13/12/2015.
  */
object ClusterView {

  object Style extends StyleSheet.Inline {
    import dsl._

    val container = style(addClassNames("container-fluid"))
    val row = style(addClassName("row"))
    val cell = style(addClassName("col-md-6"))
  }

  case class State(clusterDetails: ClusterDetails = ClusterDetails(0, 0))

  class Backend($: BackendScope[Unit, State]) {
    ClusterEventListener.onMessage(onEvent)

    def onEvent(clusterEvent: ClusterEvent): Unit = {
      println("Received event: " + clusterEvent)
    }

  }

  private[this] val component = ReactComponentB[Unit]("ClusterView").
    initialState(State()).
    backend(new Backend(_)).
    render((_, s, _) =>
      <.div(Style.container,
        <.div(Style.row,
          <.div(Style.cell, "Nodes"),
          <.div(Style.cell, "Workers")
        ),
        <.div(Style.row,
          <.div(Style.cell, s.clusterDetails.nodeCount),
          <.div(Style.cell, s.clusterDetails.workerCount)
        )
      )
    ).componentDidMount($ => {
      ClientApi.clusterDetails map { details =>
        $.modState(_.copy(clusterDetails = details))
      }
    }).buildU

  def apply() = component()

}
