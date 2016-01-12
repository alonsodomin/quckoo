package io.kairos.resolver

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import io.kairos.id.ArtifactId
import io.kairos.protocol.{ErrorResponse, ExceptionThrown, UnresolvedDependency}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scalaz._

/**
 * Created by domingueza on 23/08/15.
 */
class ResolverSpec extends TestKit(ActorSystem("ResolverSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers with MockFactory {

  import Resolver._

  import Scalaz._

  final val TestArtifactId = ArtifactId("com.example", "foo", "latest")

  val mockResolve = mockFunction[ArtifactId, Boolean, ResolutionResult]
  val moduleResolver = TestActorRef(Resolver.props(mockResolve))

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A resolver" should "return the artifact when it's valid" in {
    val jobPackage = Artifact(TestArtifactId, Seq(new URL("http://www.google.com")))

    mockResolve expects (TestArtifactId, false) returning jobPackage.successNel[ErrorResponse]

    moduleResolver ! Validate(TestArtifactId)

    val response = expectMsgType[ResolutionResult]
    response should be(jobPackage.successNel[ErrorResponse])
  }

  it should "return resolution failed error if the artifactId is not valid" in {
    val expectedResolutionFailed = UnresolvedDependency(TestArtifactId)

    mockResolve expects (TestArtifactId, false) returning expectedResolutionFailed.failureNel[Artifact]

    moduleResolver ! Validate(TestArtifactId)

    val response = expectMsgType[ResolutionResult]
    response should be(expectedResolutionFailed.failureNel[Artifact])
  }

  it should "return an error message if an exception happens when validating" in {
    val expectedException = new RuntimeException("TEST EXCEPTION")
    val expectedResult = ExceptionThrown(expectedException)

    mockResolve expects (TestArtifactId, false) throwing expectedException

    moduleResolver ! Validate(TestArtifactId)

    expectMsgType[ResolutionResult] should be (expectedResult.failureNel[Artifact])
  }

  it should "return the artifact on successful acquiring of dependencies" in {
    val jobPackage = Artifact(TestArtifactId, Seq(new URL("http://www.google.com")))

    mockResolve expects (TestArtifactId, true) returning jobPackage.successNel[ErrorResponse]

    moduleResolver ! Acquire(TestArtifactId)

    val response = expectMsgType[ResolutionResult]
    response should be(jobPackage.successNel[ErrorResponse])
  }

  it should "return resolution failed error if it can't resolve dependencies" in {
    val expectedResolutionFailed = UnresolvedDependency(TestArtifactId)

    mockResolve expects (TestArtifactId, true) returning expectedResolutionFailed.failureNel[Artifact]

    moduleResolver ! Acquire(TestArtifactId)

    val response = expectMsgType[ResolutionResult]
    response should be(expectedResolutionFailed.failureNel[Artifact])
  }

  it should "return an error message if an exception happens" in {
    val expectedException = new RuntimeException("TEST EXCEPTION")
    val expectedResult = ExceptionThrown(expectedException)

    mockResolve expects (TestArtifactId, true) throwing expectedException

    moduleResolver ! Acquire(TestArtifactId)

    expectMsgType[ResolutionResult] should be (expectedResult.failureNel[Artifact])
  }

}
