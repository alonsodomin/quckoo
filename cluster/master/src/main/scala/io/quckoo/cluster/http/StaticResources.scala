/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route

import io.quckoo.Info
import io.quckoo.console.html.index

/**
  * Created by alonsodomin on 07/07/2016.
  */
trait StaticResources {

  private[this] final val AssetsPath = "assets"
  private[this] final val PublicDir  = "public/"

  def staticResources: Route =
    pathEndOrSingleSlash {
      get {
        complete(index(Info.version))
      }
    } ~ pathPrefix(AssetsPath / Remaining) { file =>
      // optionally compresses the response with Gzip or Deflate
      // if the client accepts compressed responses
      encodeResponse {
        getFromResource(PublicDir + file)
      }
    }

}
