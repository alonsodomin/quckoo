package io.kairos.resolver

import java.net.URL

import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.util.{Success => Successful}

/**
 * Created by aalonsodominguez on 25/07/15.
 */
class ArtifactClassLoaderTest extends FlatSpec with Matchers with Inside {

  private val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")

  "An ArtifactClassLoader" should "load any class from an URL" in {
    val classLoader = new ArtifactClassLoader(Array(CommonsLoggingURL))
    val loggerClass = classLoader.loadClass("org.apache.commons.logging.Log")
    loggerClass should not be null
  }

  it should "throw ClassNotFoundException when asked to load a non existent class" in {
    val classLoader = new ArtifactClassLoader(Array(CommonsLoggingURL))
    intercept[ClassNotFoundException] {
      classLoader.loadClass("com.example.FakeClass")
    }
  }

}
