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

import java.time._
import java.nio.ByteBuffer

import cats._
import cats.data._
import cats.implicits._

import com.softwaremill.sttp._

import io.circe.syntax._

import io.quckoo._
import io.quckoo.api.TopicTag
import io.quckoo.auth.{InvalidCredentials, Passport}
import io.quckoo.client._
import io.quckoo.net.QuckooState
import io.quckoo.serialization.DataBuffer

import org.scalatest.flatspec.AsyncFlatSpec
import org.scalatest.matchers.should.Matchers

object HttpQuckooClientSpec {

  def randomPassport: Passport = {
    val Right(passport) = for {
      header    <- DataBuffer.fromString("{}").toBase64
      claims    <- DataBuffer.fromString("{}").toBase64
      signature <- DataBuffer.fromString(System.currentTimeMillis().toString).toBase64
    } yield new Passport(Map.empty, s"$header.$claims.$signature")

    passport
  }

}

class HttpQuckooClientSpec extends AsyncFlatSpec with Matchers {
  import HttpQuckooClientSpec._

  // -- Sign In requests

  "signIn" must "update clients passport when it succeeds" in {
    val username = "foo"
    val password = "bar"
    val expectedPassport = randomPassport

    val Right(encodedCreds) = DataBuffer.fromString(s"$username:$password").toBase64

    val client = HttpQuckooClientStub { stub =>
      stub.whenRequestMatches { req =>
        req.uri.path.endsWith(List("api", "auth", "login")) &&
        req.method == Method.POST &&
        req.headers.contains(AuthorizationHeader -> s"Basic $encodedCreds")
      }.thenRespond(expectedPassport.token)
    }

    client.signIn(username, password).runS(ClientState.initial).unsafeToFuture().map { returnedState =>
      returnedState.passport shouldBe expectedPassport.some
    }
  }

  it must "result in InvalidCredentials when response code is not 200" in {
    val username = "foo"
    val password = "bar"

    val Right(encodedCreds) = DataBuffer.fromString(s"$username:$password").toBase64

    val client = HttpQuckooClientStub { stub =>
      stub.whenRequestMatches { req =>
        req.uri.path.endsWith(List("api", "auth", "login")) &&
        req.method == Method.POST &&
        req.headers.contains(AuthorizationHeader -> s"Basic $encodedCreds")
      }.thenRespondWithCode(401)
    }

    client.signIn(username, password).run(ClientState.initial).attempt.unsafeToFuture().map { result =>
      result shouldBe Left(InvalidCredentials)
    }
  }

  // -- Sign Out requests

  "sing out" must "clear the passport when it succeeds" in {
    val givenPassport = randomPassport
    val clientState = ClientState(Some(givenPassport))

    val client = HttpQuckooClientStub { stub =>
      stub.whenRequestMatches { req =>
        req.uri.path.endsWith(List("api", "auth", "logout")) &&
        req.method == Method.POST &&
        req.headers.contains(AuthorizationHeader -> s"Bearer ${givenPassport.token}")
      }.thenRespondWithCode(200)
    }

    client.signOut().runS(clientState).unsafeToFuture().map { newState =>
      newState.passport shouldBe None
    }
  }

  // -- Get Cluster State

  "clusterState" must "return the cluster state details" in {
    val givenPassport = randomPassport
    val clientState = ClientState(Some(givenPassport))

    val expectedState = QuckooState()

    val client = HttpQuckooClientStub { stub =>
      stub.whenRequestMatches { req =>
        req.uri.path.endsWith(List("api", "cluster")) &&
        req.method == Method.GET &&
        req.headers.contains(AuthorizationHeader -> s"Bearer ${givenPassport.token}")
      }.thenRespond(expectedState.asJson.noSpaces)
    }

    client.clusterState.runA(clientState).unsafeToFuture().map { returnedState =>
      returnedState shouldBe expectedState
    }
  }

  // -- Register Job

  "registerJob" must "return a validated JobId when it succeeds" in {
    val givenPassport = randomPassport
    val clientState = ClientState(Some(givenPassport))

    val artifactId = ArtifactId("com.example", "bar", "latest")
    val jobId = JobId("fooId")
    val jobSpec = JobSpec("foo", jobPackage = JobPackage.jar(
      artifactId = artifactId,
      jobClass = "com.example.Job"
    ))

    val client = HttpQuckooClientStub { stub =>
      stub.whenRequestMatches { req =>
        req.uri.path.endsWith(List("api", "registry", "jobs")) &&
        req.method == Method.POST &&
        req.headers.contains(AuthorizationHeader -> s"Bearer ${givenPassport.token}")
      }.thenRespond(jobId.asJson.noSpaces)
    }

    client.registerJob(jobSpec).runA(clientState).unsafeToFuture().map { result =>
      result shouldBe jobId.validNel[QuckooError]
    }
  }

}
