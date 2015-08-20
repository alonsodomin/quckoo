package io.chronos.scheduler

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

import akka.testkit._
import io.chronos.JobSpec
import io.chronos.id.ModuleId
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
 * Created by aalonsodominguez on 18/08/15.
 */
object SchedulerSpec {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec(UUID.randomUUID(), "foo", "foo desc", TestModuleId, "com.example.Job")

}

class SchedulerSpec extends TestKit(TestActorSystem("SchedulerSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers {

  import Scheduler._
  import SchedulerSpec._

  implicit val clock = Clock.fixed(FixedInstant, ZoneUTC)

  val registryProbe = TestProbe()
  val taskQueueProbe = TestProbe()

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A scheduler" should "create an execution plan when asked to schedule a job" in {
    val scheduler = TestActorRef(Scheduler.props(
      TestActors.forwardActorProps(registryProbe.ref),
      TestActors.forwardActorProps(taskQueueProbe.ref)
    ))

    scheduler ! ScheduleJob(TestJobSpec.id)

    registryProbe.expectMsgType[Registry.GetJob].jobId should be (TestJobSpec.id)
  }

}
