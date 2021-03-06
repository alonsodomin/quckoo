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

package io.quckoo.cluster.http

import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._

import io.quckoo.api._
import io.quckoo.util.Attempt

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 30/10/2016.
  */
trait TimeoutDirectives {

  val DefaultTimeout = 2500 millis

  def extractTimeout(default: FiniteDuration): Directive1[FiniteDuration] =
    optionalHeaderValueByName(RequestTimeoutHeader).map(_.flatMap { timeoutValue =>
      Attempt(timeoutValue.toLong).map(_ millis).toOption
    } getOrElse default)

}

object TimeoutDirectives extends TimeoutDirectives
