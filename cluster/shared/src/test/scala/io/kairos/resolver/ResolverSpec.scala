package io.kairos.resolver

import java.net.URL

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import io.kairos.id.ArtifactId
import io.kairos.protocol.{ResolutionFailed, UnresolvedDependencies}
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
 * Created by domingueza on 23/08/15.
 */
class ResolverSpec extends TestKit(ActorSystem("ResolverSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers with MockFactory {

  import Resolver._

  final val TestArtifactId = ArtifactId("com.example", "foo", "latest")
  val mockResolve = mockFunction[ArtifactId, Boolean, Either[ResolutionFailed, Artifact]]
  val moduleResolver = TestActorRef(Resolver.props(mockResolve))

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A resolver" should "return the artifact when it's valid" in {
    val jobPackage = Artifact(TestArtifactId, Seq(new URL("http://www.google.com")))

    mockResolve expects (TestArtifactId, false) returning Right(jobPackage)

    moduleResolver ! Validate(TestArtifactId)

    val returnedPackage = expectMsgType[Artifact]
    returnedPackage should be(jobPackage)
  }

  it should "return resolution failed error if the artifactId is not valid" in {
    val expectedResolutionFailed = UnresolvedDependencies(Seq("com.foo.bar"))

    mockResolve expects (TestArtifactId, false) returning Left(expectedResolutionFailed)

    moduleResolver ! Validate(TestArtifactId)

    val returnedError = expectMsgType[ResolutionFailed]
    returnedError should be(expectedResolutionFailed)
  }

  it should "return an error message if an exception happens when validating" in {
    val expectedException = new RuntimeException("TEST EXCEPTION")

    mockResolve expects (TestArtifactId, false) throwing expectedException

    moduleResolver ! Validate(TestArtifactId)

    expectMsgType[ErrorResolvingModule].cause should be (expectedException)
  }

  it should "return the artifact on successful acquiring of dependencies" in {
    val jobPackage = Artifact(TestArtifactId, Seq(new URL("http://www.google.com")))

    mockResolve expects (TestArtifactId, true) returning Right(jobPackage)

    moduleResolver ! Acquire(TestArtifactId)

    val returnedPackage = expectMsgType[Artifact]
    returnedPackage should be(jobPackage)
  }

  it should "return resolution failed error if it can't resolve dependencies" in {
    val expectedResolutionFailed = UnresolvedDependencies(Seq("com.foo.bar"))

    mockResolve expects (TestArtifactId, true) returning Left(expectedResolutionFailed)

    moduleResolver ! Acquire(TestArtifactId)

    val returnedError = expectMsgType[ResolutionFailed]
    returnedError should be(expectedResolutionFailed)
  }

  it should "return an error message if an exception happens" in {
    val expectedException = new RuntimeException("TEST EXCEPTION")

    mockResolve expects (TestArtifactId, true) throwing expectedException

    moduleResolver ! Acquire(TestArtifactId)

    expectMsgType[ErrorResolvingModule].cause should be (expectedException)
  }

}
