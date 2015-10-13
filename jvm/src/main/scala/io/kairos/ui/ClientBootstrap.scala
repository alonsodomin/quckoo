package io.kairos.ui

import scalatags.Text.all._

/**
 * Created by aalonsodominguez on 11/10/2015.
 */
object ClientBootstrap {

  val boot = "io.kairos.ui.App().main(document.getElementById('contents'))"

  val skeleton = html(
    head(
      script(src := "/kairos-ui-jsdeps.js"),
      script(src := "/kairos-ui-fastopt.js"),
      link(
        rel := "stylesheet",
        href := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"
      ),
      link(
        rel := "stylesheet",
        href := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css"
      ),
      meta(name := "viewport", content := "width=device-width, initial-scale=1"),
      base(href := "/")
    ),
    body(
      `class` := "container",
      onload := boot,
      div(id := "contents")
    )
  )

}
