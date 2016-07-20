package io.quckoo.resolver

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}

import io.quckoo.fault.{Fault, MissingDependencies, UnresolvedDependency}
import io.quckoo.id.ArtifactId

import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.{ExecutionContext, Future}
import scalaz._

/**
  * Created by alonsodomin on 11/04/2016.
  */
object ResolverSpec {

  final val FooArtifactId = ArtifactId("com.example", "foo", "latest")
  final val BarArtifactId = ArtifactId("com.example", "bar", "latest")

  final val FooArtifact = Artifact(FooArtifactId, Seq.empty)

}

class ResolverSpec extends TestKit(ActorSystem("ResolverSpec")) with WordSpecLike
    with Matchers with ImplicitSender with DefaultTimeout
    with BeforeAndAfterAll with MockFactory {

  import ResolverSpec._
  import Scalaz._

  val mockResolve = mock[Resolve]
  val resolver    = TestActorRef(Resolver.props(mockResolve))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "When validating an artifact" should {

    "return a failed resolution if there are unresolved dependencies" in {
      val unresolvedDependency = UnresolvedDependency(BarArtifactId)
      val expectedFault = MissingDependencies(NonEmptyList(unresolvedDependency))
      val expectedValidation: Validation[Fault, Artifact] =
        expectedFault.failure[Artifact]

      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, false, *).
        returning(Future.successful(expectedValidation))

      resolver ! Resolver.Validate(FooArtifactId)

      expectMsg(Resolver.ResolutionFailed(FooArtifactId, expectedFault))
    }

    "return a resolved artifact if resolution succeeds" in {
      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, false, *).
        returning(Future.successful(FooArtifact.success[Fault]))

      resolver ! Resolver.Validate(FooArtifactId)

      expectMsg(Resolver.ArtifactResolved(FooArtifact))
    }

  }

  "When downloading an artifact" should {

    "return a failed resolution if there are unresolved dependencies" in {
      val unresolvedDependency = UnresolvedDependency(BarArtifactId)
      val expectedFault = MissingDependencies(NonEmptyList(unresolvedDependency))
      val expectedValidation: Validation[Fault, Artifact] =
        expectedFault.failure[Artifact]

      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, true, *).
        returning(Future.successful(expectedValidation))

      resolver ! Resolver.Download(FooArtifactId)

      expectMsg(Resolver.ResolutionFailed(FooArtifactId, expectedFault))
    }

    "return a resolved artifact if resolution succeeds" in {
      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, true, *).
        returning(Future.successful(FooArtifact.success[Fault]))

      resolver ! Resolver.Download(FooArtifactId)

      expectMsg(Resolver.ArtifactResolved(FooArtifact))
    }

  }

}
