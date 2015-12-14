package io.kairos.cluster.scheduler

import akka.testkit._
import io.kairos.JobSpec
import io.kairos.id.{JobId, ModuleId}
import io.kairos.protocol.RegistryProtocol.JobNotEnabled
import io.kairos.protocol.{RegistryProtocol, SchedulerProtocol}
import io.kairos.test.{ImplicitClock, TestActorSystem}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object SchedulerSpec {

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", "foo desc", TestModuleId, "com.example.Job")
  final val TestJobId = JobId(TestJobSpec)

}

class SchedulerSpec extends TestKit(TestActorSystem("SchedulerSpec")) with ImplicitSender with ImplicitClock
  with WordSpecLike with BeforeAndAfterAll with Matchers {

  import SchedulerProtocol._
  import SchedulerSpec._

  val registryProbe = TestProbe("registry")
  val taskQueueProbe = TestProbe("taskQueue")

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A scheduler" should {
    val scheduler = TestActorRef(Scheduler.props(
      registryProbe.ref,
      TestActors.forwardActorProps(taskQueueProbe.ref)
    ), "scheduler")

    "create an execution plan job to schedule is enabled" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[RegistryProtocol.GetJob].jobId should be (TestJobId)
      registryProbe.reply(TestJobSpec)

      expectMsgType[JobScheduled].jobId should be (TestJobId)
    }

    "should forward the registry response if the job is not enabled" in {
      scheduler ! ScheduleJob(TestJobId)

      registryProbe.expectMsgType[RegistryProtocol.GetJob].jobId should be (TestJobId)
      registryProbe.reply(JobNotEnabled(TestJobId))

      expectMsgType[JobNotEnabled].jobId should be (TestJobId)
    }
  }

}
