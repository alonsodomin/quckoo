package io.kairos.console.client.components

import org.scalajs.jquery.JQuery

import scala.scalajs.js

/**
  * Created by alonsodomin on 20/02/2016.
  */
@js.native
trait BootstrapJQuery extends JQuery {
  def modal(action: String): BootstrapJQuery = js.native
  def modal(options: js.Any): BootstrapJQuery = js.native
}
