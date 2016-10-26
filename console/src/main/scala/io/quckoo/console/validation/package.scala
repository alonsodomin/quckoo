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

package io.quckoo.console

import io.quckoo.validation._

import japgolly.scalajs.react.CallbackTo
import japgolly.scalajs.react.ScalazReact._

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 21/02/2016.
  */
package object validation {
  type ValidatorCallback[A] = ValidatorK[CallbackTo, A]

  implicit class ReactValidatorSyntax[A](self: Validator[A]) {
    def callback: ValidatorCallback[A] = self.lift[CallbackTo]
  }

}
