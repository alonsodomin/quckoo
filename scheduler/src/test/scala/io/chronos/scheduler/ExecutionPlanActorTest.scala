package io.chronos.scheduler

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

import akka.pattern._
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.chronos.id.ExecutionId
import io.chronos.protocol._
import io.chronos.{Execution, Schedule}
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

  val mockExecutionPlan = mock[ExecutionPlan]
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

}
