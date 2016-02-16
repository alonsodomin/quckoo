package io.kairos.console.client.boot

import io.kairos.console.client.pages.HomePage
import org.widok.html._
import org.widok.{PageApplication, Route, View}
import pl.metastack.metarx.Var

/**
  * Created by alonsodomin on 14/02/2016.
  */
object WidokApp extends PageApplication {

  object Routees {
    val home = Route("/", HomePage)

    val all = Seq(home)
  }

  val name = Var("")
  val hasName = name.map(_.nonEmpty)

  override def view(): View = div(
    h1("Welcome!"),
    p("Please, enter your name:"),
    text().bind(name),
    p("Hello, ", name).show(hasName),
    button("Change my name").onClick(_ => name := "tux").show(name !=== "tux"),
    button("Log out").onClick(_ => name := "").show(hasName)
  )

  override def ready(): Unit = ()

}
