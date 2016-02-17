package io.kairos.console.client.boot

import io.kairos.console.client.pages._
import org.widok.{Route, RoutingApplication}

/**
  * Created by alonsodomin on 14/02/2016.
  */
object Routees {
  val home       = Route("/", HomePage)
  val login      = Route("/login", LoginPage)
  val registry   = Route("/registry", RegistryPage)
  val executions = Route("/executions", ExecutionsPage)
  val test       = Route("/test/:param", TestPage)
  val notFound   = Route("/404", NotFound)

  val all = Set(home, login, registry, executions, test, notFound)
}

object WidokApp extends RoutingApplication(Routees.all, Routees.notFound)
