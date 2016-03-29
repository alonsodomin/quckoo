package io.quckoo.console.components

import org.scalajs.jquery.JQuery

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName

/**
  * Created by alonsodomin on 28/03/2016.
  */
@js.native
trait BootstrapNotify extends JQuery {

  @JSName("notify")
  @js.native
  def showNotification(text: String): BootstrapNotify = js.native

  @JSName("notify")
  @js.native
  def showNotification(options: js.Any): BootstrapNotify = js.native

}
