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

import akka.http.scaladsl.model.headers.{ModeledCustomHeader, ModeledCustomHeaderCompanion}
import io.quckoo.auth.XSRFToken
import io.quckoo.auth.http

import scala.util.Try

/**
  * Created by alonsodomin on 29/03/2016.
  */
object ApiTokenHeader extends ModeledCustomHeaderCompanion[ApiTokenHeader] {

  override val name: String = http.ApiTokenHeader

  override def parse(value: String): Try[ApiTokenHeader] = ???

}

class ApiTokenHeader(token: XSRFToken) extends ModeledCustomHeader[ApiTokenHeader] {

  override val companion: ModeledCustomHeaderCompanion[ApiTokenHeader] = ApiTokenHeader

  override def value(): String = token.toString

  override def renderInResponses(): Boolean = ???

  override def renderInRequests(): Boolean = ???
}
