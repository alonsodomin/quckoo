package io.kairos.console.server.boot

import scalatags.Text.all._

/**
 * Created by aalonsodominguez on 11/10/2015.
 */
object ClientBootstrap {

  val skeleton = html(
    head(
      link(
        rel := "stylesheet",
        href := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"
      ),
      link(
        rel := "stylesheet",
        href := "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap-theme.min.css"
      ),
      link(
        rel := "stylesheet",
        href := "https://maxcdn.bootstrapcdn.com/font-awesome/4.4.0/css/font-awesome.min.css"
      ),
      meta(name := "viewport", content := "width=device-width, initial-scale=1"),
      base(href := "/")
    ),
    body(id := "page",
      script(src := "/console-jsdeps.js"),
      script(src := "/console-fastopt.js"),
      script(src := "/console-launcher.js")
    )
  )

}
