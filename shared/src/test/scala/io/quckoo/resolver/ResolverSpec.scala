/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.resolver

import akka.actor.ActorSystem
import akka.testkit.{DefaultTimeout, ImplicitSender, TestActorRef, TestKit}

import cats.data.{NonEmptyList, Validated}
import cats.implicits._

import io.quckoo.{ArtifactId, QuckooError, MissingDependencies, UnresolvedDependency}

import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.{ExecutionContext, Future}

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

  val mockResolve = mock[Resolve]
  val resolver    = TestActorRef(Resolver.props(mockResolve))

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "When validating an artifact" should {

    "return a failed resolution if there are unresolved dependencies" in {
      val unresolvedDependency = UnresolvedDependency(BarArtifactId)
      val expectedFault = MissingDependencies(NonEmptyList.of(unresolvedDependency))
      val expectedValidation: Validated[QuckooError, Artifact] =
        expectedFault.invalid[Artifact]

      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, false, *).
        returning(Future.successful(expectedValidation))

      resolver ! Resolver.Validate(FooArtifactId)

      expectMsg(Resolver.ResolutionFailed(FooArtifactId, expectedFault))
    }

    "return a resolved artifact if resolution succeeds" in {
      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, false, *).
        returning(Future.successful(FooArtifact.valid[QuckooError]))

      resolver ! Resolver.Validate(FooArtifactId)

      expectMsg(Resolver.ArtifactResolved(FooArtifact))
    }

  }

  "When downloading an artifact" should {

    "return a failed resolution if there are unresolved dependencies" in {
      val unresolvedDependency = UnresolvedDependency(BarArtifactId)
      val expectedFault = MissingDependencies(NonEmptyList.of(unresolvedDependency))
      val expectedValidation: Validated[QuckooError, Artifact] =
        expectedFault.invalid[Artifact]

      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, true, *).
        returning(Future.successful(expectedValidation))

      resolver ! Resolver.Download(FooArtifactId)

      expectMsg(Resolver.ResolutionFailed(FooArtifactId, expectedFault))
    }

    "return a resolved artifact if resolution succeeds" in {
      (mockResolve.apply(_: ArtifactId, _: Boolean)(_: ExecutionContext)).
        expects(FooArtifactId, true, *).
        returning(Future.successful(FooArtifact.valid[QuckooError]))

      resolver ! Resolver.Download(FooArtifactId)

      expectMsg(Resolver.ArtifactResolved(FooArtifact))
    }

  }

}
