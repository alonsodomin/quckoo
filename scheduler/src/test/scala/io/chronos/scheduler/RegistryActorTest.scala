package io.chronos.scheduler

import java.net.URL
import java.util.UUID

import akka.testkit.{ImplicitSender, TestActorRef, TestKit}
import io.chronos.JobSpec
import io.chronos.id.ModuleId
import io.chronos.registry.Registry
import io.chronos.resolver.{JobPackage, ModuleResolver}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

/**
 * Created by domingueza on 03/08/15.
 */
object RegistryActorTest {

  val TestModuleId = ModuleId("io.chronos", "test", "latest")
  val TestJobId    = UUID.randomUUID()
  val TestJobSpec  = JobSpec(TestJobId, "foo", "", TestModuleId, "foo.JobClass")

}

class RegistryActorTest extends TestKit(TestActorSystem("RegistryActorTest")) with FlatSpecLike with Matchers
  with MockFactory with BeforeAndAfterAll with ImplicitSender {

  import RegistryActorTest._
  import io.chronos.protocol._

  val mockRegistry = mock[Registry]
  val mockModuleResolver = mock[ModuleResolver]
  val registry = TestActorRef(RegistryActor.props(mockRegistry, mockModuleResolver))

  override protected def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Registry actor" must "retrieve a job from the registry" in {
    (mockRegistry.getJob _).expects(TestJobId).returning(Option(TestJobSpec))

    registry ! GetJob(TestJobId)

    expectMsg(Some(TestJobSpec))
  }

  it must "retrieve the list of specs from the registry" in {
    val anotherJobSpec = JobSpec(UUID.randomUUID(), "bar", "", TestModuleId, "bar.JobClass")
    val expectedJobSpecs = List(TestJobSpec, anotherJobSpec)

    (mockRegistry.getJobs _).expects().returning(expectedJobSpecs)

    registry ! GetJobs

    val jobIds = receiveN(2).map { case JobSpec(id, _, _, _, _) => id }
    jobIds should contain allOf (expectedJobSpecs.head.id, expectedJobSpecs(1).id)
  }

  it must "reject the job if the dependency resolution fails" in {
    val expectedResolutionFailed = ResolutionFailed(Seq("bar"))

    (mockModuleResolver.resolve _).expects(TestJobSpec.moduleId, false).returning(Left(expectedResolutionFailed))

    registry ! RegisterJob(TestJobSpec)

    expectMsg(JobRejected(Left(expectedResolutionFailed)))
  }

  it must "accept the job if dependency resolution succeeds" in {
    val expectedJobPackage = JobPackage(TestModuleId, Seq(new URL("http://www.example.com")))

    (mockModuleResolver.resolve _).expects(TestJobSpec.moduleId, false).returning(Right(expectedJobPackage))
    (mockRegistry.registerJob _).expects(TestJobSpec)

    registry ! RegisterJob(TestJobSpec)

    expectMsg(JobAccepted(TestJobId))
  }

}
