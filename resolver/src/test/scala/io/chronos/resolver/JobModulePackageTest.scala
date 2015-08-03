package io.chronos.resolver

import java.net.URL

import io.chronos.id.JobModuleId
import org.scalamock.scalatest.MockFactory
import org.scalatest.{FlatSpec, Inside, Matchers}

import scala.collection.mutable
import scala.util.Success

/**
 * Created by aalonsodominguez on 26/07/15.
 */
class JobModulePackageTest extends FlatSpec with Matchers with MockFactory with Inside {

  class StubPackageClassLoader(urls: Array[URL]) extends PackageClassLoader(Array.empty) {
    private val classMap = mutable.Map.empty[String, Class[_]]

    def this() = this(Array.empty)

    def addClass(name: String, clazz: Class[_]): Unit = {
      classMap += (name -> clazz)
    }

    override def loadClass(name: String): Class[_] = classMap(name)

    override def getURLs: Array[URL] = urls

  }

  private val TestModuleId = JobModuleId("io.chronos.test", "package-test", "SNAPSHOT")

  "A JobModulePackage" should "return the URLs of its classpath from the class loader" in {
    val expectedUrls = Array(new URL("http://www.example.com"))

    val stubClassLoader = new StubPackageClassLoader(expectedUrls)
    val jobModulePackage = new JobModulePackage(TestModuleId, stubClassLoader)

    val returnedUrls = jobModulePackage.classpath

    returnedUrls should be (expectedUrls)
  }

  it should "load a job class from the class loader" in {
    val stubClassLoader   = new StubPackageClassLoader
    val expectedClass     = classOf[DummyJavaJob]
    val expectedClassName = expectedClass.getName
    stubClassLoader.addClass(expectedClassName, expectedClass)

    val jobModulePackage = new JobModulePackage(TestModuleId, stubClassLoader)

    val returnedClass = jobModulePackage.jobClass(expectedClassName)

    inside (returnedClass) {
      case Success(clazz) => clazz should be (expectedClass)
    }
  }

  it should "inject parameters into the job instance" in {
    val stubClassLoader   = new StubPackageClassLoader
    val expectedClass     = classOf[DummyJavaJob]
    val expectedClassName = expectedClass.getName
    stubClassLoader.addClass(expectedClassName, expectedClass)

    val parameterValue = 5
    val expectedJobInstance = new DummyJavaJob
    expectedJobInstance.value = parameterValue

    val jobModulePackage = new JobModulePackage(TestModuleId, stubClassLoader)

    val returnedInstance = jobModulePackage.newJob(expectedClassName, Map("value" -> parameterValue))

    inside (returnedInstance) {
      case Success(instance) => instance.asInstanceOf[DummyJavaJob].value should be (parameterValue)
    }
  }

}
