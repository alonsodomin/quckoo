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

package io.quckoo.client.http

import java.net.ServerSocket
import java.util.concurrent.TimeUnit

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.core.WireMockConfiguration.options

import org.scalatest.{BeforeAndAfterAll, Outcome}
import org.scalatest.flatspec.FixtureAnyFlatSpec

import scala.util.control.NonFatal

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait WireMock extends FixtureAnyFlatSpec with BeforeAndAfterAll {
  type FixtureParam = WireMockServer

  private[this] def findFreePort(): Int = {
    var port: Int = -1
    try {
      val socket = new ServerSocket(0)
      port = socket.getLocalPort
      socket.close()

      // Give some time to allow the socket to be released
      TimeUnit.MILLISECONDS.sleep(50)
    } catch {
      case NonFatal(ex) =>
        throw new RuntimeException("Could not allocate a free port.", ex)
    }
    port
  }

  override protected def withFixture(test: OneArgTest): Outcome = {
    val server = new WireMockServer(options().port(findFreePort()))
    try {
      server.start()
      withFixture(test.toNoArgTest(server))
    } finally {
      server.stop()
    }
  }
}
