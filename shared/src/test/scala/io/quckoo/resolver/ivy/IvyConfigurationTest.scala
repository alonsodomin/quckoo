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

package io.quckoo.resolver.ivy

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by domingueza on 04/08/15.
  */
object IvyConfigurationTest {

  val ConfigWithoutHome =
    ConfigFactory.parseString("""
      | work-dir = "target/work"
      | resolution-cache-dir = "target/ivy/cache"
      | repository-cache-dir = "target/ivy/local"
      |
      | repositories = []
    """.stripMargin)

  val ConfigWithHome = ConfigFactory.parseString("""
      | home = "target/home"
    """.stripMargin).withFallback(ConfigWithoutHome)

  val ExpectedWorkDir = Paths.get("target/work").toAbsolutePath.toFile
  val ExpectedResolutionCacheDir =
    Paths.get("target/ivy/cache").toAbsolutePath.toFile
  val ExpectedResolutionRepoDir =
    Paths.get("target/ivy/local").toAbsolutePath.toFile
  val ExpectedHomeDir = Paths.get("target/home").toAbsolutePath.toFile

}

class IvyConfigurationTest extends FlatSpec with Matchers {

  import IvyConfigurationTest._

  "IvyConfiguration" should "give an instance without home folder if not specified" in {
    val configuration = IvyConfiguration(ConfigWithoutHome)

    configuration.baseDir shouldBe ExpectedWorkDir
    configuration.resolutionDir shouldBe ExpectedResolutionCacheDir
    configuration.repositoryDir shouldBe ExpectedResolutionRepoDir
    configuration.ivyHome shouldBe None
    configuration.repositories shouldBe empty
  }

  it should "give an instance with a home folder if specified" in {
    val configuration = IvyConfiguration(ConfigWithHome)

    configuration.baseDir shouldBe ExpectedWorkDir
    configuration.resolutionDir shouldBe ExpectedResolutionCacheDir
    configuration.repositoryDir shouldBe ExpectedResolutionRepoDir
    configuration.ivyHome shouldBe Some(ExpectedHomeDir)
    configuration.repositories shouldBe empty
  }

}
