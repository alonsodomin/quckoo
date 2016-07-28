package io.quckoo.console.components

import io.quckoo.console.ConsoleTestState._

import org.scalajs.dom.html

/**
  * Created by alonsodomin on 28/07/2016.
  */
class NavBarObserver($: HtmlDomZipper) {

  val navItems = $.collect0n("li[role=presentation]").
    mapZippers { z =>
      val anchor = z("a")
      anchor.innerText -> anchor.domAs[html.Anchor]
    } toMap

  val activeNavItem = $.collect01("li.active").mapZippers(_("a").innerText)

}
