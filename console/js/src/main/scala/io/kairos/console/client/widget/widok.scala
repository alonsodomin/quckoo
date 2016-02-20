package io.kairos.console.client.widget

import org.scalajs.dom
import org.widok.{DOM, View, Widget}
import pl.metastack.metarx.ReadChannel

/**
  * Created by alonsodomin on 19/02/2016.
  */
object widok {

  case class Placeholder[T <: Option[Widget[_]]](value: ReadChannel[T]) extends View {
    def render(parent: dom.Node, offset: dom.Node) {
      var node = DOM.createElement("span")
      DOM.insertAfter(parent, offset, node)

      value.attach { (t: Option[Widget[_]]) =>
        t match {
          case Some(widget) =>
            parent.replaceChild(widget.rendered, node)
            node = widget.rendered
          case None =>
            val empty = DOM.createElement("span")
            parent.replaceChild(empty, node)
            node = empty
        }
      }
    }
  }

}
