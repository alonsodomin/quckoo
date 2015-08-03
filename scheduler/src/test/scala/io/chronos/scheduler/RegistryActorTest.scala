package io.chronos.scheduler

import java.net.URL
import java.util.UUID

import akka.actor.ActorSystem
import akka.pattern._
import akka.testkit.{TestActorRef, TestKit}
import akka.util.Timeout
import io.chronos.JobSpec
import io.chronos.id.ModuleId
import io.chronos.resolver.{JobPackage, ModuleResolver}
import org.scalamock.scalatest.MockFactory
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

import scala.concurrent.duration._

/**
 * Created by domingueza on 03/08/15.
 */
object RegistryActorTest {

  val TestModuleId = ModuleId("io.chronos", "test", "latest")
  val TestJobId    = UUID.randomUUID()
  val TestJobSpec  = JobSpec(TestJobId, "foo", "", TestModuleId, "foo.JobClass")

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
    (mockRegistry.getJob _).expects(TestJobId).returning(Option(TestJobSpec))

    val result = (registry ? GetJob(TestJobId)).mapTo[Option[JobSpec]]

    whenReady(result) {
      case Some(spec) => spec should be (TestJobSpec)
      case None       => fail("Expected some result from the registry actor.")
    }
  }

  it must "retrieve the list of specs from the registry" in {
    val expectedJobSpecs = List(TestJobSpec)

    (mockRegistry.getJobs _).expects().returning(expectedJobSpecs)

    val result = (registry ? GetJobs).mapTo[Seq[JobSpec]]

    whenReady(result) {
      res => res should be (expectedJobSpecs)
    }
  }

  it must "reject the job if the dependency resolution fails" in {
    val expectedResolutionFailed = ResolutionFailed(Seq("bar"))

    (mockModuleResolver.resolve _).expects(TestJobSpec.moduleId, false).returning(Left(expectedResolutionFailed))

    val result = (registry ? RegisterJob(TestJobSpec)).mapTo[JobRejected]

    whenReady(result) { res => res.cause match {
        case Left(failure) => failure should be(expectedResolutionFailed)
        case _ => fail("Expected resolution failed response from the registry.")
      }
    }
  }

  it must "accept the job if dependency resolution succeeds" in {
    val expectedJobPackage = JobPackage(TestModuleId, Seq(new URL("http://www.example.com")))

    (mockModuleResolver.resolve _).expects(TestJobSpec.moduleId, false).returning(Right(expectedJobPackage))
    (mockRegistry.registerJob _).expects(TestJobSpec)

    val result = (registry ? RegisterJob(TestJobSpec)).mapTo[JobAccepted]

    whenReady(result) { res => res.jobId should be (TestJobId) }
  }

}
