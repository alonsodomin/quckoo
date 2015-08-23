package io.chronos.scheduler

import java.net.URL
import java.util.UUID

import akka.testkit._
import io.chronos.JobSpec
import io.chronos.id.{JobId, ModuleId}
import io.chronos.protocol.{RegistryProtocol, ResolutionFailed}
import io.chronos.resolver.{JobPackage, ModuleResolver}
import org.scalatest._

/**
 * Created by domingueza on 21/08/15.
 */
object RegistrySpec {

  final val TestModuleId = ModuleId("com.example", "bar", "test")
  final val TestJobSpec = JobSpec("foo", "foo desc", TestModuleId, "com.example.Job")

  final val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")
  final val TestJobPackage = JobPackage(TestModuleId, Seq(CommonsLoggingURL))

}

class RegistrySpec extends TestKit(TestActorSystem("RegistrySpec")) with ImplicitSender
  with WordSpecLike with BeforeAndAfter with BeforeAndAfterAll with Matchers {

  import ModuleResolver._
  import RegistryProtocol._
  import RegistrySpec._

  val eventListener = TestProbe()
  var testJobId: JobId = _

  before {
    system.eventStream.subscribe(eventListener.ref, classOf[RegistryEvent])
  }

  after {
    system.eventStream.unsubscribe(eventListener.ref)
  }

  override def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

  "A job registry" should {
    val resolverProbe = TestProbe()
    val registry = TestActorRef(Registry.props(TestActors.forwardActorProps(resolverProbe.ref)))

    "reject a job if it fails to resolve its dependencies" in {
      val expectedResolutionFailed = ResolutionFailed(Seq("com.foo.bar"))

      registry ! RegisterJob(TestJobSpec)

      val resolveMsg = resolverProbe.expectMsgType[ResolveModule]
      resolveMsg.moduleId should be (TestModuleId)
      resolveMsg.download should not

      resolverProbe.reply(expectedResolutionFailed)

      expectMsgType[JobRejected].cause should be (Left(expectedResolutionFailed))
    }

    "notify that a specific job is not enabled when attempting to disabling it" in {
      val nonExistentJobId: JobId = UUID.randomUUID()
      registry ! DisableJob(nonExistentJobId)

      expectMsgType[JobNotEnabled].jobId should be (nonExistentJobId)
    }

    "return job accepted if resolution of dependencies succeeds" in {
      registry ! RegisterJob(TestJobSpec)

      val resolveMsg = resolverProbe.expectMsgType[ResolveModule]
      resolveMsg.moduleId should be (TestModuleId)
      resolveMsg.download should not

      resolverProbe.reply(TestJobPackage)

      val registryResponse = expectMsgType[JobAccepted]
      registryResponse.job should be (TestJobSpec)
      testJobId = registryResponse.jobId
    }

    "return the registered job spec when asked for it" in {
      registry ! GetJob(testJobId)

      expectMsg(TestJobSpec)
    }

    "return a collection of all the registered jobs when asked for it" in {
      registry ! GetJobs

      expectMsg(Seq(TestJobSpec))
    }

    "disable a job that has been previously registered and populate the event to the event stream" in {
      registry ! DisableJob(testJobId)

      expectMsgType[JobDisabled].jobId should be (testJobId)
      eventListener.expectMsgType[JobDisabled].jobId should be (testJobId)
    }

    "return a job not enabled message when asked for a job after disabling it" in {
      registry ! GetJob(testJobId)

      expectMsgType[JobNotEnabled].jobId should be (testJobId)
    }

    "return an empty collection when there are not enabled jobs" in {
      registry ! GetJobs

      expectMsg(Seq.empty[JobSpec])
    }
  }

}
