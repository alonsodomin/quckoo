package io.chronos.resolver

import java.io.File
import java.net.URL
import java.util.concurrent.Callable

import io.chronos.id.JobModuleId
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.util.{Failure, Success => Successful}

/**
 * Created by aalonsodominguez on 25/07/15.
 */
class JobModulePackageTest extends FlatSpec with Matchers with Inside {

  private val ivyCacheDir = new File(System.getProperty("user.home"), ".ivy2/cache")

  private val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")
  private val ChronosExamplesURL = new File(ivyCacheDir, "io.chronos/examples_2.11/jars/examples_2.11-0.1.0.jar").toURI.toURL

  private val TestModuleId = JobModuleId("io.chronos.test", "package-test", "SNAPSHOT")

  "A JobModulePackage" should "load any class from an URL" in {
    val jobPackage = JobModulePackage(TestModuleId, List(CommonsLoggingURL))
    val loggerClass = jobPackage.loadClass("org.apache.commons.logging.Log")
    loggerClass should matchPattern { case Successful(_) => }
  }

  it should "fail when asked to load a non existent class" in {
    val jobPackage = JobModulePackage(TestModuleId, List(CommonsLoggingURL))
    val failedClass = jobPackage.loadClass("com.example.FakeClass")
    failedClass should matchPattern { case Failure(_) => }
  }

  it should "load a job class from an URL" in {
    val jobPackage = JobModulePackage(TestModuleId, List(ChronosExamplesURL))
    val jobClass = jobPackage.jobClass("io.chronos.examples.parameters.PowerOfNJob")
    inside (jobClass) {
      case Successful(clazz) =>
        assert(classOf[Callable[_]].isAssignableFrom(clazz))
    }
  }

  it should "instantiate a job instance from the package" in {
    val jobPackage = JobModulePackage(TestModuleId, List(CommonsLoggingURL, ChronosExamplesURL))
    val jobInstance = jobPackage.newJob("io.chronos.examples.parameters.PowerOfNJob", Map("n" -> 2))
    inside (jobInstance) {
      case Successful(job) =>
        job.getClass.getField("n").get(job) should be (2)
    }
  }

}
