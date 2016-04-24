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

package io.quckoo.time

import org.widok.moment.{Date => MDate, Moment}

/**
  * Created by alonsodomin on 22/12/2015.
  */
object MomentJSTimeSource {

  def default: TimeSource = new MomentJSTimeSource(() => Moment.utc())

  def fixed(millis: Double): TimeSource = new MomentJSTimeSource(() => Moment.utc(millis))

  object Implicits {

    implicit val default = MomentJSTimeSource.default

  }

}

class MomentJSTimeSource private (moment: () => MDate) extends TimeSource {

  def currentDateTime: DateTime = new MomentJSDateTime(moment())

}
