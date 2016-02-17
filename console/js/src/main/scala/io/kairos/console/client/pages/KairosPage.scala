package io.kairos.console.client.pages

import org.widok._
import org.widok.bindings.Bootstrap._

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait KairosPage extends Page {

  def header: Widget[_]

  def body(route: InstantiatedRoute): View

  def render(route: InstantiatedRoute): Future[View] = Future {
    Inline(
      header,
      Container(body(route))
    )
  }

}
