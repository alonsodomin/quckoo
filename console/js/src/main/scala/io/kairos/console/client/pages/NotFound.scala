package io.kairos.console.client.pages

import org.widok.{InstantiatedRoute, Page, View}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 17/02/2016.
  */
case class NotFound() extends Page {
  override def render(route: InstantiatedRoute): Future[View] = ???
}
