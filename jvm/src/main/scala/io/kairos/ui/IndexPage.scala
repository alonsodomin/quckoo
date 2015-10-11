package io.kairos.ui

import scalatags.Text.all._

/**
 * Created by aalonsodominguez on 11/10/2015.
 */
object IndexPage {

  val boot = "io.kairos.ui.App().main(document.getElementById('contents'))"

  val skeleton = html(
    head(
      script(src := "/kairos-ui-jsdeps.js"),
      script(src := "/kairos-ui-fastopt.js"),
      link(
        rel := "stylesheet",
        href := "http://yui.yahooapis.com/pure/0.6.0/pure-min.css"
      )
    ),
    body(
      onload := boot,
      div(id := "contents")
    )
  )

}
