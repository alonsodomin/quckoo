/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.client.http

import cats.effect.IO
import io.quckoo.auth.{NotAuthorized, Passport, SessionExpired}
import io.quckoo.client.QuckooClient
import io.quckoo.client.core.DriverBackend
import org.scalajs.dom.XMLHttpRequest

/**
  * Created by alonsodomin on 20/09/2016.
  */
package object dom {
  type HttpResponseHandler[A] = PartialFunction[XMLHttpRequest, IO[A]]

  implicit val backend: DriverBackend[HttpProtocol] = HttpDOMBackend
  val HttpDOMQuckooClient                           = QuckooClient[HttpProtocol]

  def handleResponse[A](handler: HttpResponseHandler[A]): XMLHttpRequest => IO[A] = {
    def defaultHandler: XMLHttpRequest => IO[A] = res => {
      if (res.status == 401) IO.raiseError(SessionExpired)
      else if (res.status == 403) IO.raiseError(NotAuthorized)
      else if (res.status > 400) IO.raiseError(new Exception(res.statusText))
      else IO.raiseError(new Exception("Invalid response."))
    }

    handler.applyOrElse(_, defaultHandler)
  }

}
