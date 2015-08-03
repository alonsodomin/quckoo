package io.chronos.scheduler

import java.util.UUID

import akka.actor.ActorSystem
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import io.chronos.JobSpec
import io.chronos.id.ModuleId
import io.chronos.resolver.ModuleResolver
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}

/**
 * Created by domingueza on 03/08/15.
 */
object RegistryActorTest {
  val TestModuleId = ModuleId("io.chronos", "test", "latest")
}

class RegistryActorTest(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with BeforeAndAfter
  with BeforeAndAfterAll with MockFactory {

  import RegistryActorTest._
  import io.chronos.protocol._

  def this() = this(ActorSystem("RegistryActorTest", ConfigFactory.load("test-application")))

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Registry actor" must {
    val mockRegistry = mock[Registry]
    val mockModuleResolver = mock[ModuleResolver]
    val registryActor = system.actorOf(RegistryActor.props(mockRegistry, mockModuleResolver))

    "retrieve a job from the registry" in {
      val jobId = UUID.randomUUID()
      val expectedJobSpec = JobSpec(jobId, "foo", "", TestModuleId, "foo.JobClass")

      (mockRegistry.getJob _).expects(jobId).returning(Option(expectedJobSpec))

      registryActor ! GetJob(jobId)
      expectMsg(Option(expectedJobSpec))
    }
  }

}
