package io.kairos.console.client.pages

import org.widok._
import org.widok.bindings.Bootstrap._
import org.widok.bindings.HTML
import pl.metastack.metarx.Var

/**
  * Created by alonsodomin on 15/02/2016.
  */
case class HomePage() extends KairosPage with NavigationHeader with SecurePage {
  val activeNodes = Var(0)
  val inactiveNodes = Var(0)
  val activeWorkers = Var(0)
  val inactiveWorkers = Var(0)

  def clusterInfoPanel: View = Inline(
    Panel(
      Panel.Heading("Nodes"),
      Panel.Body(
        HTML.Table(
          HTML.Table.Body(
            HTML.Table.Row(
              HTML.Table.Column("Active"),
              HTML.Table.Column(activeNodes)
            ),
            HTML.Table.Row(
              HTML.Table.Column("Inctive"),
              HTML.Table.Column(inactiveNodes)
            )
          )
        )
      )
    ),
    Panel(
      Panel.Heading("Workers"),
      Panel.Body(
        HTML.Table(
          HTML.Table.Body(
            HTML.Table.Row(
              HTML.Table.Column("Active"),
              HTML.Table.Column(activeWorkers)
            ),
            HTML.Table.Row(
              HTML.Table.Column("Inactive"),
              HTML.Table.Column(inactiveWorkers)
            )
          )
        )
      )
    )
  )

  override def body(route: InstantiatedRoute): View = {
    log(s"Page 'home' loaded with route '$route'")
    Inline(
      Grid.Column(clusterInfoPanel).column(Size.Medium, 4),
      Grid.Column("Hello!").column(Size.Medium, 8)
    )
  }

  override def destroy(): Unit = {
    log("Home page left behind")
  }
}
