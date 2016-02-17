package io.kairos.console.client.pages

import org.widok._
import org.widok.bindings.Bootstrap._

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait KairosPage extends Page {
  import KairosPage._

  private[this] val readyCallbacks = ListBuffer.empty[Node => ReadyResult]

  def header: Widget[_]

  def body(route: InstantiatedRoute): View

  def render(route: InstantiatedRoute): Future[View] = Future {
    Inline(
      header,
      Container(body(route))
    )
  }

  final def whenReady(f: Node => ReadyResult): Unit = {
    readyCallbacks += f
  }

  final override def ready(node: Node): Unit = {
    import ReadyResult._

    readyCallbacks.takeWhile { callback =>
      callback(node) == Continue
    }
  }

}

object KairosPage {

  sealed trait ReadyResult
  object ReadyResult {
    case object ShortCircuit extends ReadyResult
    case object Continue extends ReadyResult
  }

}
