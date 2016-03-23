package io.quckoo.resolver.ivy

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by domingueza on 04/08/15.
 */
object IvyConfigurationTest {

  val ConfigWithoutHome = ConfigFactory.parseString(
    """
      |resolver {
      | work-dir = "target/work"
      | resolution-cache-dir = "target/ivy/cache"
      | repository-cache-dir = "target/ivy/local"
      |
      | repositories = []
      |}
    """.stripMargin)

  val ConfigWithHome = ConfigFactory.parseString(
    """
      |resolver {
      | home = "target/home"
      |}
    """.stripMargin).withFallback(ConfigWithoutHome)

  val ExpectedWorkDir            = Paths.get("target/work").toAbsolutePath.toFile
  val ExpectedResolutionCacheDir = Paths.get("target/ivy/cache").toAbsolutePath.toFile
  val ExpectedResolutionRepoDir  = Paths.get("target/ivy/local").toAbsolutePath.toFile
  val ExpectedHomeDir            = Paths.get("target/home").toAbsolutePath.toFile

}

class IvyConfigurationTest extends FlatSpec with Matchers {

  import IvyConfigurationTest._

  "IvyConfiguration" should "give an instance without home folder if not specified" in {
    val configuration = IvyConfiguration(ConfigWithoutHome)

    configuration.baseDir should be (ExpectedWorkDir)
    configuration.resolutionDir should be (ExpectedResolutionCacheDir)
    configuration.repositoryDir should be (ExpectedResolutionRepoDir)
    configuration.ivyHome should be (None)
    assert(configuration.repositories.isEmpty)
  }

  it should "give an instance with a home folder if specified" in {
    val configuration = IvyConfiguration(ConfigWithHome)

    configuration.baseDir should be (ExpectedWorkDir)
    configuration.resolutionDir should be (ExpectedResolutionCacheDir)
    configuration.repositoryDir should be (ExpectedResolutionRepoDir)
    configuration.ivyHome should be (Some(ExpectedHomeDir))
    assert(configuration.repositories.isEmpty)
  }

}
