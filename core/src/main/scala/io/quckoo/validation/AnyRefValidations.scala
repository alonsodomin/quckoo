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

package io.quckoo.validation

import io.quckoo.fault.{NotNull, ValidationFault}

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
trait AnyRefValidations {
  import Scalaz._

  def notNull[T <: AnyRef](t: T)(msg: => String): Validation[ValidationFault, T] = {
    if (t == null) NotNull(msg).failure[T]
    else t.success[NotNull]
  }

}
