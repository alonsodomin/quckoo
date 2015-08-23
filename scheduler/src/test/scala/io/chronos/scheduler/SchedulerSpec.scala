package io.chronos.scheduler

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

import akka.testkit._
import io.chronos.JobSpec
import io.chronos.id.ModuleId
import io.chronos.protocol.RegistryProtocol.JobNotEnabled
import io.chronos.protocol.{RegistryProtocol, SchedulerProtocol}
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object SchedulerSpec {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobId = UUID.randomUUID()
  final val TestJobSpec = JobSpec("foo", "foo desc", TestModuleId, "com.example.Job")

}

class SchedulerSpec extends TestKit(TestActorSystem("SchedulerSpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfterAll with Matchers {

  import SchedulerProtocol._
  import SchedulerSpec._

  implicit val clock = Clock.fixed(FixedInstant, ZoneUTC)

  val registryProbe = TestProbe("registry")
  val taskQueueProbe = TestProbe("taskQueue")

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A scheduler" should {
    val timeout = 1 second

    val scheduler = TestActorRef(Scheduler.props(
      registryProbe.ref,
      TestActors.forwardActorProps(taskQueueProbe.ref),
      timeout
    ), "scheduler")

    "create an execution plan job to schedule is enabled" in {
      within(timeout + (1 second)) {
        scheduler ! ScheduleJob(TestJobId)

        registryProbe.expectMsgType[RegistryProtocol.GetJob].jobId should be (TestJobId)
        registryProbe.reply(TestJobSpec)
      }

      expectMsgType[JobScheduled].jobId should be (TestJobId)
    }

    "should forward the registry response if the job is not enabled" in {
      within(timeout + (2 second)) {
        scheduler ! ScheduleJob(TestJobId)

        registryProbe.expectMsgType[RegistryProtocol.GetJob].jobId should be (TestJobId)
        registryProbe.reply(JobNotEnabled(TestJobId))
      }

      expectMsgType[JobNotEnabled].jobId should be (TestJobId)
    }

    "should inform schedule failed if communication with registry fails" in {
      // will intentionally throw an exception
      registryProbe.setAutoPilot(TestActor.KeepRunning)

      scheduler ! ScheduleJob(TestJobId)

      expectMsgType[JobFailedToSchedule]
    }
  }

}
