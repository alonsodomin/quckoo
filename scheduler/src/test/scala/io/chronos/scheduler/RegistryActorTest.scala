package io.chronos.scheduler

import java.util.UUID

import akka.actor.ActorSystem
import akka.cluster.Cluster
import akka.testkit.TestKit
import com.typesafe.config.ConfigFactory
import io.chronos.JobSpec
import io.chronos.id.ModuleId
import io.chronos.resolver.ModuleResolver
import org.scalamock.scalatest.MockFactory
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll, WordSpecLike}

import scala.concurrent.duration._

/**
 * Created by domingueza on 03/08/15.
 */
object RegistryActorTest {

  val ClusterConfig = ConfigFactory.load("test-cluster")
  val ClientConfig = ConfigFactory.parseString(
    """
      |akka.actor.provider = "akka.remote.RemoteActorRefProvider"
      |akka.remote.netty.tcp.port=0
    """.stripMargin)

  val TestModuleId = ModuleId("io.chronos", "test", "latest")

}

class RegistryActorTest(_system: ActorSystem) extends TestKit(_system) with WordSpecLike with BeforeAndAfter
  with BeforeAndAfterAll with MockFactory {

  import io.chronos.protocol._

  def this() = this(ActorSystem("RegistryActorTest", RegistryActorTest.ClusterConfig))

  override def afterAll: Unit = {
    system.shutdown()
    system.awaitTermination()
  }

  "A Registry actor" must {
    import RegistryActorTest._

    val mockRegistry = mock[Registry]
    val mockModuleResolver = mock[ModuleResolver]

    val clusterAddress = Cluster(system).selfAddress
    Cluster(system).join(clusterAddress)

    val registry = system.actorOf(RegistryActor.props(mockRegistry, mockModuleResolver), "registry")

    "retrieve a job from the registry" in {
      val jobId = UUID.randomUUID()
      val expectedJobSpec = JobSpec(jobId, "foo", "", TestModuleId, "foo.JobClass")

      (mockRegistry.getJob _).expects(jobId).returning(Option(expectedJobSpec))

      within(10.seconds) {
        awaitAssert {
          registry ! GetJob(jobId)
          expectMsg(Option(expectedJobSpec))
        }
      }
    }
  }

}
