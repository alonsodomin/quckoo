package io.kairos.console.client.pages

import io.kairos.console.client.boot.Routees
import org.widok.Widget
import org.widok.bindings.Bootstrap._
import org.widok.bindings.HTML
import pl.metastack.metarx.Var

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait DefaultHeader {
  val closed = Var(true)

  def header: Widget[_] = {
    NavigationBar(Container(
      NavigationBar.Header(
        NavigationBar.Toggle(closed),
        NavigationBar.Collapse(closed)(NavigationBar.Brand("Kairos").url(Routees.home()))
      ),
      NavigationBar.Navigation(
        NavigationBar.Elements(
          HTML.List.Item(HTML.Anchor("Registry").url(Routees.registry())),
          HTML.List.Item(HTML.Anchor("Executions").url(Routees.executions()))
        )
      )
    ))
  }

}
