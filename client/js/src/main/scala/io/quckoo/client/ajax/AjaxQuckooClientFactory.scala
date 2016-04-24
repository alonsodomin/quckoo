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

package io.quckoo.client.ajax

import io.quckoo.auth.http._
import io.quckoo.client.{QuckooClient, QuckooClientFactory}
import io.quckoo.serialization.Base64._
import org.scalajs.dom.ext.Ajax

import scala.concurrent.{ExecutionContext, Future}

/**
  * Created by alonsodomin on 26/03/2016.
  */
object AjaxQuckooClientFactory extends QuckooClientFactory {

  def autoConnect(): Option[QuckooClient] = Cookie(AuthCookie).
    map(token => new AjaxQuckooClient(Some(token)))

  def connect(username: String, password: String)(implicit ec: ExecutionContext): Future[QuckooClient] = {
    val authentication = s"$username:$password".getBytes("UTF-8").toBase64
    val hdrs = Map(AuthorizationHeader -> s"Basic $authentication")

    Ajax.post(LoginURI, headers = hdrs).
      map { xhr => new AjaxQuckooClient(Some(xhr.responseText)) }
  }

}
