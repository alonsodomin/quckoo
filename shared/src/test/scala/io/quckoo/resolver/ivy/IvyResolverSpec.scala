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

package io.quckoo.resolver.ivy

import java.nio.file.Files

import cats.data.NonEmptyList
import cats.effect.IO
import cats.implicits._

import io.quckoo._
import io.quckoo.reflect.Artifact
import io.quckoo.resolver._
import io.quckoo.resolver.config.IvyConfig

import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

/**
  * Created by alonsodomin on 23/01/2016.
  */
object IvyResolverSpec {

  private final val TestBasePath = Files.createTempDirectory("ivyresolve")

  final val TestIvyConfig = IvyConfig(
    TestBasePath.toFile,
    Files.createTempDirectory(TestBasePath, "resolution-cache").toFile,
    Files.createTempDirectory(TestBasePath, "repository-cache").toFile
  )

  final val InvalidArtifactId = ArtifactId("com.example", "example", "latest")
  final val TestArtifactId = ArtifactId("io.quckoo", "quckoo-core_2.11", "0.1.0")

}

class IvyResolverSpec extends FlatSpec with GivenWhenThen with Matchers {
  import IvyResolverSpec._

  "IvyResolve" should "accumulate all the errors of the resolve operation" in {
    Given("An Ivy resolver")
    implicit val ivyResolver = IvyResolver(TestIvyConfig)

    And("an expected unresolved dependency")
    val expectedUnresolvedDependency = UnresolvedDependency(InvalidArtifactId)

    And("the expected result as accumulation of errors")
    val expectedResult = NonEmptyList.of(expectedUnresolvedDependency).invalid[ArtifactId]

    When("Attempting to validate the artifact")
    val result = ops.validate(InvalidArtifactId).to[IO].unsafeRunSync()

    Then("Result should be the expected errors")
    result shouldBe expectedResult
  }

  it should "return an artifact when the resolution report contains no errors" in {
    Given("An Ivy resolver")
    implicit val ivyResolver = IvyResolver(TestIvyConfig)

    And("an expected resolved artifact")
    val expectedArtifact = Artifact(TestArtifactId, List.empty)

    When("attempting to download the artifacts")
    val result = ops.download(TestArtifactId).to[IO].unsafeRunSync()

    Then("the returned validation should contain the expected artifact")
    result shouldBe expectedArtifact.validNel[DependencyError]
  }

}
