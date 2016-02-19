package io.kairos.console.client.pages

import io.kairos.console.client.boot.Routees
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.security.ClientAuth
import org.scalajs.dom
import org.widok._
import org.widok.bindings.Bootstrap._
import org.widok.bindings.{FontAwesome => fa, HTML}
import pl.metastack.metarx.Var

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait NavigationHeader extends ClientAuth { self: KairosPage =>
  val closed = Var(true)

  val navigationRoutes: Map[Seq[View], Route] = Map(
    Seq[View](Glyphicon.Dashboard(), "Dashboard") -> Routees.dashboard,
    Seq[View](fa.Book(), "Registry")              -> Routees.registry,
    Seq[View](fa.Bolt(), "Executions")            -> Routees.dashboard
  )

  def logout(event: dom.Event) = {
    ClientApi.logout() map { _ =>
      Routees.login().go()
    }
  }

  def header(current: InstantiatedRoute): Widget[_] = {
    val links = navigationRoutes.map {
      case (views, route) =>
        HTML.List.Item(HTML.Anchor(views: _*).
          url(route())).cssState(current.route == route, "active")
    } toSeq

    NavigationBar(Container(
      NavigationBar.Header(
        NavigationBar.Toggle(closed),
        NavigationBar.Collapse(closed)(NavigationBar.Brand("Kairos"))
      ),
      NavigationBar.Navigation(
        NavigationBar.Elements(links: _*)
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
