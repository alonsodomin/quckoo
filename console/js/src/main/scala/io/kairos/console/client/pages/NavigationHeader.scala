package io.kairos.console.client.pages

import io.kairos.console.client.boot.Routees
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.security.ClientAuth
import org.scalajs.dom
import org.widok.Widget
import org.widok.bindings.Bootstrap._
import org.widok.bindings.{FontAwesome => fa, HTML}
import pl.metastack.metarx.Var

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait NavigationHeader extends ClientAuth { self: KairosPage =>
  val closed = Var(true)

  def logout(event: dom.Event) = {
    ClientApi.logout() map { _ =>
      Routees.login().go()
    }
  }

  def header: Widget[_] = {
    NavigationBar(Container(
      NavigationBar.Header(
        NavigationBar.Toggle(closed),
        NavigationBar.Collapse(closed)(NavigationBar.Brand("Kairos").url(Routees.home()))
      ),
      NavigationBar.Navigation(
        NavigationBar.Elements(
          HTML.List.Item(HTML.Anchor(
            fa.Book(),
            "Registry"
          ).url(Routees.registry())),
          HTML.List.Item(HTML.Anchor(
            fa.Bolt(),
            "Executions"
          ).url(Routees.executions()))
        )
      ),
      NavigationBar.Right(
        NavigationBar.Elements(
          HTML.List.Item(
            HTML.Anchor(fa.SignOut(), "Logout").onClick(logout)
          )
        )
      )
    )).position(NavigationBar.Position.Top)
  }

}
