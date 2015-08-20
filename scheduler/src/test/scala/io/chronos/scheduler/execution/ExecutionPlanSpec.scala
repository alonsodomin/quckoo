package io.chronos.scheduler.execution

import java.time.{Clock, Instant, ZoneId}
import java.util.UUID

import akka.actor._
import akka.testkit._
import io.chronos.Trigger
import io.chronos.scheduler.Registry
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

/**
 * Created by aalonsodominguez on 20/08/15.
 */
object ExecutionPlanSpec {

  final val FixedInstant = Instant.ofEpochMilli(893273L)
  final val ZoneUTC = ZoneId.of("UTC")

  class WatchAndForwardActor(executionPlan: ActorRef, listener: ActorRef)
    extends Actor {

    context.watch(executionPlan)

    override def receive = {
      case message => listener ! message
    }

  }

}

class ExecutionPlanSpec extends TestKit(ActorSystem("ExecutionPlanSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll with Matchers with MockFactory {

  import ExecutionPlanSpec._

  implicit val clock = Clock.fixed(FixedInstant, ZoneUTC)

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "An execution plan" should "kill itself if job is not enabled" in {
    val trigger = mock[Trigger]
    val executionProps: ExecutionPlan.ExecutionProps =
      (planId, jobSpec) => TestActors.echoActorProps

    val receiver = TestProbe()
    val executionPlan = TestActorRef(ExecutionPlan.props(trigger)(executionProps))
    TestActorRef(Props(classOf[WatchAndForwardActor], executionPlan, receiver.ref))

    executionPlan ! Registry.JobNotEnabled(UUID.randomUUID())

    //receiver.expectTerminated(executionPlan)
  }



}
