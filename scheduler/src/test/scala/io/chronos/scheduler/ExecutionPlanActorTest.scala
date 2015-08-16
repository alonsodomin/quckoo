package io.chronos.scheduler

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.chronos.protocol._
import org.scalamock.scalatest.MockFactory
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 03/08/15.
 */
object ExecutionPlanActorTest {
  val FixedInstant = Instant.ofEpochMilli(98329923L)
  val FixedZoneId  = ZoneId.systemDefault()
}

class ExecutionPlanActorTest extends TestKit(TestActorSystem("ExecutionPlanActorTest")) with FlatSpecLike
  with Matchers with MockFactory with BeforeAndAfterAll with ScalaFutures {

  import ExecutionPlanActorTest._

  implicit val clock = Clock.fixed(FixedInstant, FixedZoneId)
  implicit val timeout = Timeout(1 second)

  val mockExecutionPlan = mock[ExecutionCache]
  val actor = TestActorRef(ExecutionPlanActor.props(mockExecutionPlan))
  
  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A schedule plan actor" must "be able to schedule jobs" in {
    val jobId = UUID.randomUUID()
    val schedule = Schedule(jobId)

    val executionId: ExecutionId = ((jobId, 1), 1)
    val expectedExecution = Execution(executionId)

    (mockExecutionPlan.schedule(_ : Schedule)(_ : Clock)).expects(schedule, *).returning(expectedExecution)

    val result = (actor ? ScheduleJob(schedule)).mapTo[ScheduleJobAck]

    whenReady(result) { _.executionId should be (executionId) }
  }

  it must "be able to reschedule jobs" in {
    val jobId = UUID.randomUUID()
    val scheduleId: ScheduleId = (jobId, 1)

    val executionId: ExecutionId = (scheduleId, 1)
    val expectedExecution = Execution(executionId)

    (mockExecutionPlan.reschedule(_ : ScheduleId)(_ : Clock)).expects(scheduleId, *).returning(expectedExecution)

    val result = (actor ? RescheduleJob(scheduleId)).mapTo[ScheduleJobAck]

    whenReady(result) { _.executionId should be (executionId) }
  }

  it must "retrieve a specific schedule" in {
    val jobId = UUID.randomUUID()
    val scheduleId: ScheduleId = (jobId, 1)

    val expectedSchedule = Schedule(jobId)

    (mockExecutionPlan.getSchedule _).expects(scheduleId).returning(Some(expectedSchedule))

    val result = (actor ? GetSchedule(scheduleId)).mapTo[Option[Schedule]]

    whenReady(result) {
      case Some(s) => s should be (expectedSchedule)
      case _       => fail("Expected receiving an instance of a schedule")
    }
  }

  it must "query the existent schedules" in {
    val jobId = UUID.randomUUID()
    val scheduleId: ScheduleId = (jobId, 1)
    val schedule = Schedule(jobId)

    val expectedSchedules = List((scheduleId, schedule))

    (mockExecutionPlan.getScheduledJobs _).expects().returning(expectedSchedules)

    val result = (actor ? GetScheduledJobs).mapTo[Seq[(ScheduleId, Schedule)]]

    whenReady(result) { _ should be (expectedSchedules) }
  }

  it must "retrieve a specific execution" in {
    val jobId = UUID.randomUUID()
    val scheduleId: ScheduleId = (jobId, 1)
    val executionId: ExecutionId = (scheduleId, 1)

    val expectedExecution = Execution(executionId)

    (mockExecutionPlan.getExecution _).expects(executionId).returning(Some(expectedExecution))

    val result = (actor ? GetExecution(executionId)).mapTo[Option[Execution]]

    whenReady(result) {
      case Some(x) => x should be (expectedExecution)
      case _       => fail("Expected receiving an instance of an execution")
    }
  }

  it must "query the existent executions" in {
    val jobId = UUID.randomUUID()
    val scheduleId: ScheduleId = (jobId, 1)
    val executionId: ExecutionId = (scheduleId, 1)

    val expectedExecutions = List(Execution(executionId))
    val expectedFilter: Execution => Boolean = _ => true

    (mockExecutionPlan.getExecutions _).expects(expectedFilter).returning(expectedExecutions)

    val result = (actor ? GetExecutions(expectedFilter)).mapTo[Seq[Execution]]

    whenReady(result) { _ should be (expectedExecutions) }
  }

}
