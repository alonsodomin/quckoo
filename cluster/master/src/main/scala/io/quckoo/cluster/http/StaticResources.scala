package io.quckoo.cluster.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

/**
  * Created by alonsodomin on 07/07/2016.
  */
trait StaticResources {

  private[this] final val ResourcesDir = "quckoo"

  def staticResources: Route = get {
    pathEndOrSingleSlash {
      getFromResource(s"$ResourcesDir/index.html")
    } ~ getFromResourceDirectory(ResourcesDir)
  }

}
