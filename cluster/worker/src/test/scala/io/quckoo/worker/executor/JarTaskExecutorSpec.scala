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

package io.quckoo.worker.executor

import java.net.URL
import java.util.UUID

import akka.testkit._

import io.quckoo._
import io.quckoo.resolver.{Artifact, Resolver}
import io.quckoo.worker.core.{TaskExecutor, WorkerContext}
import io.quckoo.testkit.QuckooActorSuite

import org.scalamock.scalatest.MockFactory

import scalaz.NonEmptyList

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object JarTaskExecutorSpec {

  final val TestTaskId: TaskId = TaskId(UUID.randomUUID())
  final val TestJobClass = "com.example.FooClass"
  final val TestArtifactId = ArtifactId("com.example", "test", "latest")

}

class JarTaskExecutorSpec extends QuckooActorSuite("JobExecutorSpec")
  with ImplicitSender with DefaultTimeout with MockFactory {

  import JarTaskExecutorSpec._

  "A job executor" must {

    "fail if instantiation of the job failed" in {
      val resolverProbe = TestProbe("resolverProbe")
      val resolvedArtifact = Artifact(TestArtifactId, Seq(new URL("http://www.example.com")))

      val workerContext = mock[WorkerContext]
      val executor = TestActorRef(
        JarTaskExecutor.props(workerContext, TestTaskId, JarJobPackage(TestArtifactId, TestJobClass)),
        "failing-executor"
      )

      val expectedException = new ClassNotFoundException(TestJobClass)

      (workerContext.resolver _).expects().returning(resolverProbe.ref)

      executor ! TaskExecutor.Run

      resolverProbe.expectMsg(Resolver.Download(TestArtifactId))
      resolverProbe.reply(Resolver.ArtifactResolved(resolvedArtifact))

      expectMsgType[TaskExecutor.Failed].error shouldBe ExceptionThrown.from(expectedException)
    }

    "reply with a failure message when can not resolve the artifact of a task" in {
      val resolverProbe = TestProbe("resolverProbe")

      val workerContext = mock[WorkerContext]
      val executor = TestActorRef(
        JarTaskExecutor.props(workerContext, TestTaskId, JarJobPackage(TestArtifactId, TestJobClass)),
        "non-resolving-executor"
      )

      val dependencyError = UnresolvedDependency(ArtifactId("com.example", "bar", "latest"))
      val expectedFault = MissingDependencies(NonEmptyList(dependencyError))

      (workerContext.resolver _).expects().returning(resolverProbe.ref)

      executor ! TaskExecutor.Run

      resolverProbe.expectMsg(Resolver.Download(TestArtifactId))
      resolverProbe.reply(Resolver.ResolutionFailed(TestArtifactId, expectedFault))

      expectMsgType[TaskExecutor.Failed].error shouldBe expectedFault
    }
  }

}
