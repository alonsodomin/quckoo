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

package io.quckoo.worker.config

import com.typesafe.config.ConfigFactory

import io.quckoo.test.TryAssertions

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alonsodomin on 04/11/2016.
  */
class WorkerSettingsSpec extends FlatSpec with Matchers with TryAssertions {

  "WorkerSettings" should "load the default configuration settings" in {
    val config = ConfigFactory.load()

    ifSuccessful(WorkerSettings(config)) { settings =>
      settings.worker.contactPoints should not be empty
    }
  }

}
