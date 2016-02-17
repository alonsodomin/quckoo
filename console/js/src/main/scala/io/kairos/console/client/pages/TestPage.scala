package io.kairos.console.client.pages

import org.widok.{Inline, InstantiatedRoute, View}
import pl.metastack.metarx.Channel

/**
  * Created by alonsodomin on 17/02/2016.
  */
case class TestPage() extends KairosPage with DefaultHeader {
  val query = Channel[String]()

  override def body(route: InstantiatedRoute): View = {
    query := route.args("param")
    Inline("Received parameter: ", route.args("param"))
  }

}
