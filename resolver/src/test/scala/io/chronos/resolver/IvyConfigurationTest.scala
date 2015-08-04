package io.chronos.resolver

import java.nio.file.Paths

import com.typesafe.config.ConfigFactory
import org.scalatest.{FlatSpec, Matchers}

/**
 * Created by domingueza on 04/08/15.
 */
object IvyConfigurationTest {

  val ConfigWithoutHome = ConfigFactory.parseString(
    """
      |ivy {
      | workDir = "target/work"
      | cacheDir = "target/cache"
      |}
    """.stripMargin)

  val ConfigWithHome = ConfigFactory.parseString(
    """
      |ivy {
      | home = "target/home"
      |}
    """.stripMargin).withFallback(ConfigWithoutHome)

  val ExpectedWorkDir  = Paths.get("target/work").toAbsolutePath
  val ExpectedCacheDir = Paths.get("target/cache").toAbsolutePath
  val ExpectedHomeDir  = Paths.get("target/home").toAbsolutePath

}

class IvyConfigurationTest extends FlatSpec with Matchers {

  import IvyConfigurationTest._

  "IvyConfiguration factory" should "give an instance without home folder if not specified" in {
    val configuration = IvyConfiguration(ConfigWithoutHome)

    configuration.baseDir should be (ExpectedWorkDir)
    configuration.cacheDir should be (ExpectedCacheDir)
    configuration.ivyHome should be (None)
  }

  it should "give an instance with a home folder if specified" in {
    val configuration = IvyConfiguration(ConfigWithHome)

    configuration.baseDir should be (ExpectedWorkDir)
    configuration.cacheDir should be (ExpectedCacheDir)
    configuration.ivyHome should be (Some(ExpectedHomeDir))
  }

}
