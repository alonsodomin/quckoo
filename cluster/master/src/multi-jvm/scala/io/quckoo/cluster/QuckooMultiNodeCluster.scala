package io.quckoo.cluster

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.stream.ActorMaterializer
import akka.testkit.ImplicitSender

import com.typesafe.config.ConfigFactory

import io.quckoo.cluster.core.QuckooGuardian
import io.quckoo.cluster.journal.QuckooTestJournal
import io.quckoo.id.ArtifactId
import io.quckoo.multijvm.MultiNodeClusterSpec
import io.quckoo.protocol.client._
import io.quckoo.test.ImplicitClock

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise}

/**
 * Created by domingueza on 26/08/15.
 */
object QuckooNodesConfig extends MultiNodeConfig {
  val scheduler = role("scheduler")
  val registry  = role("registry")

  commonConfig(debugConfig(on = false))

  nodeConfig(scheduler)(ConfigFactory.parseString("akka.cluster.roles=[scheduler]").
    withFallback(MultiNodeClusterSpec.clusterConfig))
  nodeConfig(registry)(ConfigFactory.parseString("akka.cluster.roles=[registry]").
    withFallback(MultiNodeClusterSpec.clusterConfig))
}

class QuckooMultiNodeClusterSpecMultiJvmNode1 extends QuckooMultiNodeCluster
class QuckooMultiNodeClusterSpecMultiJvmNode2 extends QuckooMultiNodeCluster

object QuckooMultiNodeCluster {

  val GuardianName = "quckoo"
  val TestArtifactId = ArtifactId("io.kairos", "example-jobs_2.11", "0.1.0-SNAPSHOT")

}

abstract class QuckooMultiNodeCluster extends MultiNodeSpec(QuckooNodesConfig) with ImplicitSender
  with MultiNodeClusterSpec with ImplicitClock {

  import QuckooNodesConfig._
  import QuckooMultiNodeCluster._

  implicit val materializer = ActorMaterializer()
  val journal = new QuckooTestJournal

  "A Chronos cluster" must {
    val settings = QuckooClusterSettings(system)

    "send connect commands from one node to the other one" in {
      awaitClusterUp(registry, scheduler)

      runOn(registry) {
        val bootPromise = Promise[Unit]
        system.actorOf(QuckooGuardian.props(settings, journal, bootPromise), GuardianName)
        Await.ready(bootPromise.future, Duration.Inf)

        enterBarrier("deployed")

        val schedulerGuardian = system.actorSelection(node(scheduler) / "user" / GuardianName)
        schedulerGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")
      }

      runOn(scheduler) {
        val bootPromise = Promise[Unit]
        system.actorOf(QuckooGuardian.props(settings, journal, bootPromise), GuardianName)
        Await.ready(bootPromise.future, Duration.Inf)

        enterBarrier("deployed")

        val registryGuardian = system.actorSelection(node(registry) / "user" / "chronos")
        registryGuardian ! Connect

        expectMsg(Connected)

        enterBarrier("connected")
      }

      enterBarrier("finished")
    }

  }

}
