package io.chronos.multijvm

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender
import com.typesafe.config.ConfigFactory
import io.chronos.JobSpec
import io.chronos.cluster.Chronos
import io.chronos.id.ModuleId
import io.chronos.protocol.RegistryProtocol
import io.chronos.scheduler.TaskQueue
import io.chronos.test.ImplicitClock

/**
 * Created by domingueza on 26/08/15.
 */
object ChronosNodesConfig extends MultiNodeConfig {
  val scheduler = role("scheduler")
  val registry  = role("registry")

  commonConfig(debugConfig(on = false).withFallback(MultiNodeClusterSpec.clusterConfig))

  nodeConfig(scheduler)(ConfigFactory.parseString("akka.cluster.roles=[scheduler]"))
  nodeConfig(registry)(ConfigFactory.parseString("akka.cluster.roles=[registry]"))
}

class ChronosClusterSpecMultiJvmNode1 extends ChronosCluster
class ChronosClusterSpecMultiJvmNode2 extends ChronosCluster

object ChronosCluster {

  val TestModuleId = ModuleId("io.chronos", "example-jobs_2.11", "0.1.0-SNAPSHOT")

}

abstract class ChronosCluster extends MultiNodeSpec(ChronosNodesConfig) with ImplicitSender
  with MultiNodeClusterSpec with ImplicitClock {

  import ChronosCluster._
  import ChronosNodesConfig._
  import RegistryProtocol._

  override def initialParticipants: Int = roles.size

  "A Chronos cluster" must {

    "registry jobs in one node and fetch them from the other one" in {
      awaitClusterUp(registry, scheduler)

      runOn(registry) {
        system.actorOf(Chronos.props(TaskQueue.props()), "chronos")
        enterBarrier("deployed")
      }

      runOn(scheduler) {
        //system.actorOf(Chronos.props(TaskQueue.props()), "chronos")
        enterBarrier("deployed")

        val registryRef = system.actorSelection(node(registry) / "user" / "chronos" / "registry")
        registryRef ! RegisterJob(JobSpec("examples", "examples", TestModuleId, "invalid.class.Name"))

        //expectMsgType[JobAccepted].job.moduleId should be (TestModuleId)
      }
    }

  }

}
