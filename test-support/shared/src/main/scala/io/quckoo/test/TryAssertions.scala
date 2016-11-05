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

package io.quckoo.test

import org.scalactic.source
import org.scalatest.{Assertions, Inside}

import scala.util.{Failure, Success, Try}

/**
  * Created by alonsodomin on 04/11/2016.
  */
trait TryAssertions extends Inside { this: Assertions =>

  def ifSuccessful[T, U](result: Try[T])(f: T => U)(implicit pos: source.Position): U = {
    inside(result) {
      case Success(value) => f(value)
      case Failure(ex)    => fail(ex)
    }
  }

}
