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

package io.quckoo.console

import org.scalajs.jquery.{JQuery, JQueryStatic}

import scala.language.implicitConversions

/**
  * Created by alonsodomin on 02/03/2017.
  */
package object libs {

  // JQuery plugins

  implicit def toBootstrapJQuery(jq: JQuery): BootstrapJQuery =
    jq.asInstanceOf[BootstrapJQuery]

  implicit def jq2Notify(jq: JQueryStatic): BootstrapNotify =
    jq.asInstanceOf[BootstrapNotify]

}
