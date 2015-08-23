package io.chronos.scheduler

import java.net.URL
import java.util.UUID

import akka.testkit.{ImplicitSender, TestActorRef, TestKit, TestProbe}
import io.chronos.JobSpec
import io.chronos.id.{JobId, ModuleId}
import io.chronos.protocol.{RegistryProtocol, ResolutionFailed}
import io.chronos.resolver.{DependencyResolver, JobPackage}
import org.scalamock.scalatest.MockFactory
import org.scalatest._

/**
 * Created by domingueza on 21/08/15.
 */
object RegistrySpec {

  final val TestJobId: JobId = UUID.randomUUID()

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec(TestJobId, "foo", "foo desc", TestModuleId, "com.example.Job")

  final val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")
  final val TestJobPackage = JobPackage(TestModuleId, Seq(CommonsLoggingURL))

}

class RegistrySpec extends TestKit(TestActorSystem("RegistrySpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll with Matchers with MockFactory {

  import RegistryProtocol._
  import RegistrySpec._

  val eventListener = TestProbe()

  before {
    system.eventStream.subscribe(eventListener.ref, classOf[RegistryEvent])
  }

  after {
    system.eventStream.unsubscribe(eventListener.ref)
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A job registry" should {
    val dependencyResolverMock = mock[DependencyResolver]
    val registry = TestActorRef(Registry.props(dependencyResolverMock))

    "reject a job if it fails to resolve its dependencies" in {
      val expectedResolutionFailed = ResolutionFailed(Seq("com.foo.bar"))

      (dependencyResolverMock.resolve _).expects(TestModuleId, false).returning(Left(expectedResolutionFailed))

      registry ! RegisterJob(TestJobSpec)

      expectMsgType[JobRejected].cause should be (Left(expectedResolutionFailed))
    }

    "notify that a specific job is not enabled when attempting to disabling it" in {
      registry ! DisableJob(TestJobId)

      expectMsgType[JobNotEnabled].jobId should be (TestJobId)
    }

    "return job accepted if resolution of dependencies succeeds" in {
      (dependencyResolverMock.resolve _).expects(TestModuleId, false).returning(Right(TestJobPackage))

      registry ! RegisterJob(TestJobSpec)

      expectMsgType[JobAccepted].job should be (TestJobSpec)
    }

    "return the registered job spec when asked for it" in {
      registry ! GetJob(TestJobId)

      expectMsg(TestJobSpec)
    }

    "return a collection of all the registered jobs when asked for it" in {
      registry ! GetJobs

      expectMsg(Seq(TestJobSpec))
    }

    "disable a job that has been previously registered and populate the event to the event stream" in {
      registry ! DisableJob(TestJobId)

      expectMsgType[JobDisabled].jobId should be (TestJobId)
      eventListener.expectMsgType[JobDisabled].jobId should be (TestJobId)
    }

    "return a job not enabled message when asked for a job after disabling it" in {
      registry ! GetJob(TestJobId)

      expectMsgType[JobNotEnabled].jobId should be (TestJobId)
    }

    "return an empty collection when there are not enabled jobs" in {
      registry ! GetJobs

      expectMsg(Seq.empty[JobSpec])
    }
  }

}
