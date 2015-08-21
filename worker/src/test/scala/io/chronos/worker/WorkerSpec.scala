package io.chronos.worker

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.client.ClusterClient.Send
import akka.testkit._
import io.chronos.cluster.protocol.WorkerProtocol
import io.chronos.cluster.{Task, path}
import io.chronos.id.ModuleId
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by domingueza on 21/08/15.
 */
object WorkerSpec {

  val TestJobClass = "com.example.FooClass"
  val TestModuleId = ModuleId("com.example", "foo", "latest")

}

class WorkerSpec extends TestKit(ActorSystem("WorkerSpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfterAll with Matchers {

  import WorkerProtocol._
  import WorkerSpec._

  val clusterClientProbe = TestProbe()

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A worker" should {
    val executorProbe = TestProbe()
    val executorProps = TestActors.forwardActorProps(executorProbe.ref)

    val worker = TestActorRef(Worker.props(clusterClientProbe.ref, executorProps, 1 day))

    "auto-register itself with the task queue" in {
      val registration = clusterClientProbe.expectMsgType[Send]
      registration.path should be (path.TaskQueue)
      registration.msg should matchPattern { case RegisterWorker(_) => }
    }

    "request work to the task queue when the queue says that there are pending tasks" in {
      worker ! TaskReady

      val taskRequest = clusterClientProbe.expectMsgType[Send]
      taskRequest.path should be (path.TaskQueue)
      taskRequest.msg should matchPattern { case RequestTask(_) => }
    }

    "instruct the executor to execute a task that arrives to it when it's idle" in {
      val task = Task(UUID.randomUUID(), TestModuleId, Map.empty, TestJobClass)
      worker ! task

      executorProbe.expectMsgType[JobExecutor.Execute].task should be (task)
    }
  }

}
