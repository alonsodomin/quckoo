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

package io.quckoo.cluster.config

import com.typesafe.config.ConfigFactory

import io.quckoo.test.TryAssertions

import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 04/11/2016.
  */
object ClusterConfigSpec {

  object taskQueue {
    final val DefaultMaxWorkTimeout = 10 minutes
  }

  object http {
    final val DefaultInterface = "0.0.0.0"
    final val DefaultPort = 8095
  }

}

class ClusterConfigSpec extends FlatSpec with Matchers with TryAssertions {
  import ClusterConfigSpec._

  "ClusterConfig" should "be able to load default configuration" in {
    val config = ConfigFactory.load()
    val clusterConfigAttempt = ClusterConfig(config)

    ifSuccessful(clusterConfigAttempt) { clusterConf =>
      clusterConf.taskQueue.maxWorkTimeout shouldBe taskQueue.DefaultMaxWorkTimeout

      clusterConf.http.bindInterface shouldBe http.DefaultInterface
      clusterConf.http.bindPort shouldBe http.DefaultPort
    }
  }

}
