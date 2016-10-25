package io.quckoo.console.scheduler

import org.scalajs.dom.html

import CronTriggerInputState._

/**
  * Created by alonsodomin on 03/09/2016.
  */
class CronTriggerInputObserver($: HtmlDomZipper) {

  val expressionInput = $("#cronTrigger").domAs[html.Input]
  val parseError = $.collect01("#cronParseError").
    mapZippers(_.domAs[html.Div]).map(_.innerHTML)

}
