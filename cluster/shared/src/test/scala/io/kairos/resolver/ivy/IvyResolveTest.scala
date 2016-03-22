package io.kairos.resolver.ivy

import java.io.File
import java.net.URL
import java.util.concurrent.ForkJoinPool

import io.kairos.fault.{DownloadFailed, UnresolvedDependency, Fault}
import io.kairos.id.ArtifactId
import io.kairos.resolver.Artifact
import io.kairos.Validated
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{Artifact => IvyArtifact, ModuleDescriptor}
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.{ArtifactDownloadReport, ResolveReport}
import org.apache.ivy.core.resolve.{IvyNode, ResolveOptions}
import org.mockito.{Matchers => Match, Mockito}
import org.scalatest.concurrent.PatienceConfiguration.Timeout
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.mockito.MockitoSugar
import org.scalatest.time.SpanSugar
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

import scala.concurrent.ExecutionContext
import scalaz._

/**
  * Created by alonsodomin on 23/01/2016.
  */
object IvyResolveTest {

  final val TestArtifactId = ArtifactId("io.kairos", "test", "latest")
  final val TestModuleRevisionId = ModuleRevisionId.newInstance("io.kairos", "test", "latest")

  class MockableResolveReport extends ResolveReport(null)

}

class IvyResolveTest extends FlatSpec with GivenWhenThen with Matchers with ScalaFutures with MockitoSugar with SpanSugar {
  import IvyResolveTest._
  import Match._
  import Mockito._

  implicit val executionContext = ExecutionContext.fromExecutorService(ForkJoinPool.commonPool())

  "IvyResolve" should "accumulate all the errors of the resolve operation" in {
    Given("An Ivy resolver")
    val mockIvy = mock[Ivy]
    val ivyResolve = new IvyResolve(mockIvy)

    val mockReport = mock[MockableResolveReport]
    val mockUnresolvedNode = mock[IvyNode]

    when(mockIvy.resolve(any(classOf[ModuleDescriptor]), any(classOf[ResolveOptions]))).
      thenReturn(mockReport)
    when(mockReport.getUnresolvedDependencies).thenReturn(Array(mockUnresolvedNode))

    And("an expected unresolved dependency")
    val expectedUnresolvedDependency = UnresolvedDependency(TestArtifactId)

    when(mockUnresolvedNode.getId).thenReturn(TestModuleRevisionId)

    And("an artifact that fails to download")
    val failedDownloadName = "org.example#test"
    val failedDownloadArtifact = mock[IvyArtifact]
    val failedDownloadReport = new ArtifactDownloadReport(failedDownloadArtifact)
    val expectedDownloadFailed = DownloadFailed(failedDownloadName)

    when(mockReport.getFailedArtifactsReports).
      thenReturn(Array[ArtifactDownloadReport](failedDownloadReport))
    when(failedDownloadArtifact.getName).thenReturn(failedDownloadName)

    And("the expected result as accumulation of errors")
    import Scalaz._
    val validatedDep: Validated[Artifact] = expectedUnresolvedDependency.failureNel[Artifact]
    val validatedDown: Validated[Artifact] = expectedDownloadFailed.failureNel[Artifact]
    val expectedResult = (validatedDep |@| validatedDown) { case (_, a) => a }

    When("Attempting to resolve the artifact")
    whenReady(ivyResolve(TestArtifactId, download = false), Timeout(5 seconds)) { result =>
      Then("Result should be the expected errors")
      result should be (expectedResult)

      verify(mockIvy).resolve(any(classOf[ModuleDescriptor]), any(classOf[ResolveOptions]))
      verify(mockReport).getUnresolvedDependencies
      verify(mockReport).getFailedArtifactsReports
      verify(mockUnresolvedNode).getId
      verify(failedDownloadArtifact).getName
    }

  }

  it should "return an artifact when the resolution report contains no errors" in {
    Given("An Ivy resolver")
    val mockIvy = mock[Ivy]
    val ivyResolve = new IvyResolve(mockIvy)
    val mockReport = mock[MockableResolveReport]

    And("some temporary files representing artifact downloads")
    val unpackedLocalFile = File.createTempFile("kairos", "IvyResolveTest_unpacked")
    val localFile = File.createTempFile("kairos", "IvyResolveTest_local")

    val artifactReport1 = {
      val r = new ArtifactDownloadReport(null)
      r.setUnpackedLocalFile(unpackedLocalFile)
      r
    }
    val artifactReport2 = {
      val r = new ArtifactDownloadReport(null)
      r.setLocalFile(localFile)
      r
    }

    And("an artifact URL")
    val artifactUrl = new URL("http://www.example.com")
    val artifactReport3 = {
      val artifact = mock[IvyArtifact]
      val r = new ArtifactDownloadReport(artifact)
      when(artifact.getUrl).thenReturn(artifactUrl)
      r
    }

    when(mockIvy.resolve(any(classOf[ModuleDescriptor]), any(classOf[ResolveOptions]))).
      thenReturn(mockReport)
    when(mockReport.getUnresolvedDependencies).thenReturn(Array[IvyNode]())
    when(mockReport.getFailedArtifactsReports).thenReturn(Array[ArtifactDownloadReport]())
    when(mockReport.getAllArtifactsReports).thenReturn(Array[ArtifactDownloadReport](
      artifactReport1, artifactReport2, artifactReport3
    ))

    And("an expected resolved artifact")
    val expectedArtifact = Artifact(TestArtifactId, Seq(
      unpackedLocalFile.toURI.toURL, localFile.toURI.toURL, artifactUrl
    ))

    When("attempting to download the artifacts")
    whenReady(ivyResolve(TestArtifactId, download = true), Timeout(2 seconds)) { result =>
      import Scalaz._

      Then("the returned validation should contain the expected artifact")
      result should be (expectedArtifact.successNel[Fault])

      verify(mockIvy).resolve(any(classOf[ModuleDescriptor]), any(classOf[ResolveOptions]))
      verify(mockReport).getUnresolvedDependencies
      verify(mockReport).getFailedArtifactsReports
      verify(mockReport).getAllArtifactsReports
    }
  }

}
