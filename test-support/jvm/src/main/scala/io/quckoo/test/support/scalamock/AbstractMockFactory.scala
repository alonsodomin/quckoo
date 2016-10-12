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

package io.quckoo.test.support.scalamock

import org.scalamock.MockFactoryBase
import org.scalatest.exceptions.{StackDepthException, TestFailedException}
import org.scalatest.{Failed, Outcome, SuiteMixin, TestSuite}

/**
  * Created by alonsodomin on 04/09/2016.
  */
trait AbstractMockFactory extends SuiteMixin with MockFactoryBase with TestSuite {

  type ExpectationException = TestFailedException

  abstract override def withFixture(test: NoArgTest): Outcome = {

    if (autoVerify) {
      withExpectations {
        val outcome = super.withFixture(test)
        outcome match {
          case Failed(throwable) =>
            // MockFactoryBase does not know how to handle ScalaTest Outcome.
            // Throw error that caused test failure to prevent hiding it by
            // "unsatisfied expectation" exception (see issue #72)
            throw throwable
          case _ => outcome
        }
      }
    } else {
      super.withFixture(test)
    }
  }

  protected def newExpectationException(message: String, methodName: Option[Symbol]) =
    new TestFailedException((_: StackDepthException) => Some(message), None, failedCodeStackDepthFn(methodName))

  protected var autoVerify = true
}
