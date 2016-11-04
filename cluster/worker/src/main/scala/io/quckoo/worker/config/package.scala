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

package io.quckoo.worker

import akka.actor.{AddressFromURIString, RootActorPath}

import pureconfig._

/**
  * Created by alonsodomin on 04/11/2016.
  */
package object config {

  implicit val contactPointConfig: ConfigConvert[ContactPoint] = ConfigConvert.fromNonEmptyString {
    case AddressFromURIString(addr) => new ContactPoint(RootActorPath(addr) / "system" / "receptionist")
    case str                        => throw new IllegalArgumentException(s"Invalid contact point: $str")
  }

}
