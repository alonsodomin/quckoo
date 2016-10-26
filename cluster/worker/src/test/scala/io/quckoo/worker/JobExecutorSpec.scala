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

package io.quckoo.worker

import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit._

import io.quckoo.Task
import io.quckoo.fault.ExceptionThrown
import io.quckoo.id.{ArtifactId, TaskId}
import io.quckoo.resolver.Artifact

import org.scalatest._

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object JobExecutorSpec {

  final val TestTaskId: TaskId = UUID.randomUUID()
  final val TestJobClass = "com.example.FooClass"
  final val TestArtifactId = ArtifactId("com.example", "test", "latest")

}

class JobExecutorSpec extends TestKit(ActorSystem("JobExecutorSpec")) with FlatSpecLike with Matchers
  with BeforeAndAfterAll with ImplicitSender with DefaultTimeout {

  import JobExecutorSpec._

  val jobExecutor = TestActorRef(JobExecutor.props, self)

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A job executor actor" must "fail if instantiation of the job failed" in {
    //val params = Map("a" -> 7)
    val task = Task(TestTaskId, TestArtifactId, TestJobClass)

    val expectedException = new ClassNotFoundException(TestJobClass)
    val failingPackage = Artifact(TestArtifactId, Seq(new URL("http://www.example.com")))

    jobExecutor ! JobExecutor.Execute(task, failingPackage)

    expectMsgType[JobExecutor.Failed].error should be(ExceptionThrown.from(expectedException))
  }

}
