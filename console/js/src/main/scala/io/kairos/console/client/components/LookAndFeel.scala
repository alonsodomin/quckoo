package io.kairos.console.client.components

import scalacss.Defaults._
import scalacss.mutable.Register

/**
  * Created by alonsodomin on 20/02/2016.
  */
class LookAndFeel(implicit r: Register) extends StyleSheet.Inline()(r) {
  import ContextStyle._
  import dsl._

  val global = Domain.ofValues(default, primary, success, info, warning, danger)
  val context = Domain.ofValues(success, info, warning, danger)

  def from[A](domain: Domain[A], base: String) = styleF(domain) { opt =>
    styleS(addClassNames(base, s"$base-$opt"))
  }

  def wrap(classNames: String*) = style(addClassNames(classNames: _*))

  val buttonOpt = from(global, "btn")
  val button    = buttonOpt(default)

  val panelOpt     = from(global, "panel")
  val panel        = panelOpt(default)
  val panelHeading = wrap("panel-heading")
  val panelBody    = wrap("panel-body")

  val labelOpt = from(global, "label")
  val label    = labelOpt(default)

  val alert    = from(context, "alert")

  object modal {
    val modal   = wrap("modal")
    val fade    = wrap("fade")
    val dialog  = wrap("modal-dialog")
    val content = wrap("modal-content")
    val header  = wrap("modal-header")
    val body    = wrap("modal-body")
    val footer  = wrap("modal-footer")
  }

}
