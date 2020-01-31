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

package io.quckoo.testkit

import akka.actor.ActorSystem
import akka.testkit.TestKit

import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike

import slogging._

/**
  * Created by alonsodomin on 17/02/2017.
  */
abstract class QuckooActorSuite(name: String)
    extends TestKit(ActorSystem(name)) with AnyWordSpecLike with Matchers with BeforeAndAfterAll {

  override protected def beforeAll(): Unit = {
    LoggerConfig.factory = SLF4JLoggerFactory()
    LoggerConfig.level = LogLevel.DEBUG
  }

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

}
