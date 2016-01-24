package io.kairos.resolver.ivy

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
      | workDir = "target/work"
      | cacheDir = "target/cache"
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

  val ExpectedWorkDir  = Paths.get("target/work").toAbsolutePath.toFile
  val ExpectedCacheDir = Paths.get("target/cache").toAbsolutePath.toFile
  val ExpectedHomeDir  = Paths.get("target/home").toAbsolutePath.toFile

}

class IvyConfigurationTest extends FlatSpec with Matchers {

  import IvyConfigurationTest._

  "IvyConfiguration factory" should "give an instance without home folder if not specified" in {
    val configuration = IvyConfiguration(ConfigWithoutHome)

    configuration.baseDir should be (ExpectedWorkDir)
    configuration.cacheDir should be (ExpectedCacheDir)
    configuration.ivyHome should be (None)
    assert(configuration.repositories.isEmpty)
  }

  it should "give an instance with a home folder if specified" in {
    val configuration = IvyConfiguration(ConfigWithHome)

    configuration.baseDir should be (ExpectedWorkDir)
    configuration.cacheDir should be (ExpectedCacheDir)
    configuration.ivyHome should be (Some(ExpectedHomeDir))
    assert(configuration.repositories.isEmpty)
  }

}
