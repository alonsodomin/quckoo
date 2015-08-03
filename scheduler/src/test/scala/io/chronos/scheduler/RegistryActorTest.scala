package io.chronos.scheduler

import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern._
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.chronos.JobSpec
import io.chronos.id.ModuleId
import io.chronos.resolver.ModuleResolver
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

/**
 * Created by domingueza on 03/08/15.
 */
object RegistryActorTest {

  val TestModuleId = ModuleId("io.chronos", "test", "latest")

}

class RegistryActorTest extends TestKit(ActorSystem("RegistryActorTest")) with FlatSpecLike with Matchers
  with MockFactory with ScalaFutures with BeforeAndAfterAll {

  import RegistryActorTest._
  import io.chronos.protocol._

  val mockRegistry = mock[Registry]
  val mockModuleResolver = mock[ModuleResolver]
  val registry = TestActorRef(RegistryActor.props(mockRegistry, mockModuleResolver))

  implicit val timeout = Timeout(1 second)

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Registry actor" must "retrieve a job from the registry" in {
    val jobId = UUID.randomUUID()
    val expectedJobSpec = JobSpec(jobId, "foo", "", TestModuleId, "foo.JobClass")

    (mockRegistry.getJob _).expects(jobId).returning(Option(expectedJobSpec))

    val result = (registry ? GetJob(jobId)).mapTo[Option[JobSpec]]

    whenReady(result) {
      case Some(spec) => spec should be (expectedJobSpec)
      case None       => fail("Expected some result from the registry actor.")
    }
  }

  it must "retrieve the list of specs from the registry" in {
    val jobId = UUID.randomUUID()
    val expectedJobSpecs = List(JobSpec(jobId, "foo", "", TestModuleId, "foo.JobClass"))

    (mockRegistry.getJobs _).expects().returning(expectedJobSpecs)

    val result = (registry ? GetJobs).mapTo[Seq[JobSpec]]

    whenReady(result) {
      res => res should be (expectedJobSpecs)
    }
  }

}
