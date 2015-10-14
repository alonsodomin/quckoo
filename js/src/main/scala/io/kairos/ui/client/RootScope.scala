package io.kairos.ui.client

import org.scalajs.dom

/**
 * Created by alonsodomin on 13/10/2015.
 */
object RootScope {

  private def rawCookies: Map[String, String] = {
    import dom.document

    val pairs = document.cookie.split("; ").map { c =>
      val namePair = c.split("=")
      namePair(0) -> namePair(1)
    }

    Map(pairs: _*)
  }

  def cookie(name: String): Option[String] =
    rawCookies.get(name)

}
