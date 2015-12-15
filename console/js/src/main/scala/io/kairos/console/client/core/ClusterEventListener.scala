package io.kairos.console.client.core

import org.scalajs.dom
import org.scalajs.dom.raw.MessageEvent

/**
  * Created by alonsodomin on 14/12/2015.
  */
object ClusterEventListener {

  val EventURI = "cluster/events"

  private[this] lazy val source = new dom.EventSource(EventURI)

  def onMessage(f: ClusterEvent => Unit): Unit = {
    source.addEventListener[MessageEvent]("message", evt => {
      import upickle.default._

      val clusterEvt = read[ClusterEvent](evt.data.toString)
      f(clusterEvt)
    })
  }

}
