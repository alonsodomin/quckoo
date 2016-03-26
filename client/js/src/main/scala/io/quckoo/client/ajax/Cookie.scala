package io.quckoo.client.ajax

import org.scalajs.dom

/**
 * Created by alonsodomin on 13/10/2015.
 */
object Cookie {

  private[this] def rawCookies: Map[String, String] = {
    import dom.document

    val pairs = document.cookie.split("; ").map { c =>
      val namePair = c.split("=")
      namePair(0) -> namePair(1)
    }

    Map(pairs: _*)
  }

  def apply(name: String): Option[String] =
    rawCookies.get(name)

}
