package io.chronos.scheduler

import java.io.File
import java.time.Clock

import akka.actor.{Actor, ActorSystem, PoisonPill, Props, RootActorPath}
import akka.cluster.Cluster
import akka.contrib.pattern.DistributedPubSubMediator.{Subscribe, SubscribeAck}
import akka.contrib.pattern.{ClusterClient, ClusterSingletonManager, DistributedPubSubExtension}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import io.chronos.example.PowerOfNJobDef
import io.chronos.protocol.SchedulerProtocol
import io.chronos.{Work, WorkResult}
import org.apache.commons.io.FileUtils
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike, Matchers}

import scala.concurrent.duration._

object DistributedWorkerSpec {

  val clusterConfig = ConfigFactory.parseString("""
    akka.actor.provider = "akka.cluster.ClusterActorRefProvider"
    akka.remote.netty.tcp.port=0
    akka.persistence {
      journal.leveldb {
        native = off
        dir = "target/test-journal"
      }
      snapshot-store.local.dir = "target/test-snapshots"
    }
                                                """)

  val workerConfig = ConfigFactory.parseString("""
    akka.actor.provider = "akka.remote.RemoteActorRefProvider"
    akka.remote.netty.tcp.port=0
                                               """)

  class FlakyWorkExecutor extends Actor {
    var i = 0

    override def postRestart(reason: Throwable): Unit = {
      i = 3
      super.postRestart(reason)
    }

    def receive = {
      case n: Int =>
        i += 1
        if (i == 3) throw new RuntimeException("Flaky worker")
        if (i == 5) context.stop(self)

        val n2 = n * n
        val result = s"$n * $n = $n2"
        sender() ! Worker.WorkerDone(result)
    }
  }
}

class DistributedWorkerSpec(_system: ActorSystem)
  extends TestKit(_system)
  with Matchers
  with FlatSpecLike
  with BeforeAndAfterAll
  with ImplicitSender {

  import DistributedWorkerSpec._

  val workTimeout = 3.seconds

  def this() = this(ActorSystem("DistributedWorkerSpec", DistributedWorkerSpec.clusterConfig))

  val backendSystem: ActorSystem = {
    val config = ConfigFactory.parseString("akka.cluster.roles=[backend]").withFallback(clusterConfig)
    ActorSystem("DistributedWorkerSpec", config)
  }

  val workerSystem: ActorSystem = ActorSystem("DistributedWorkerSpec", workerConfig)

  val storageLocations = List(
    "akka.persistence.journal.leveldb.dir",
    "akka.persistence.snapshot-store.local.dir"
  ).map(s => new File(system.settings.config.getString(s)))

  override def beforeAll: Unit = {
    storageLocations.foreach(dir => FileUtils.deleteDirectory(dir))
  }

  override def afterAll: Unit = {
    system.shutdown()
    backendSystem.shutdown()
    workerSystem.shutdown()

    system.awaitTermination()
    backendSystem.awaitTermination()
    workerSystem.awaitTermination()

    storageLocations.foreach(dir => FileUtils.deleteDirectory(dir))
  }

  "Distributed workers" should "perform work and publish results" in {
    val clock = Clock.systemUTC()

    val clusterAddress = Cluster(backendSystem).selfAddress
    Cluster(backendSystem).join(clusterAddress)
    backendSystem.actorOf(ClusterSingletonManager.props(Scheduler.props(clock, workTimeout), "active",
      PoisonPill, Some("backend")), "scheduler")

    val initialContacts = Set(
      workerSystem.actorSelection(RootActorPath(clusterAddress) / "user" / "receptionist"))
    val clusterClient = workerSystem.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    for (n <- 1 to 3)
      workerSystem.actorOf(Worker.props(clusterClient, Props[JobExecutor], 1.second), "worker-" + n)
    val flakyWorker = workerSystem.actorOf(Worker.props(clusterClient, Props[FlakyWorkExecutor], 1.second), "flaky-worker")

    Cluster(system).join(clusterAddress)
    val frontend = system.actorOf(Props[Facade], "frontend")

    val results = TestProbe()
    DistributedPubSubExtension(system).mediator ! Subscribe(Scheduler.ResultsTopic, results.ref)
    expectMsgType[SubscribeAck]

    // might take a while for things to get connected
    within(10.seconds) {
      awaitAssert {
        frontend ! SchedulerProtocol.ScheduleJob(PowerOfNJobDef)
        expectMsg(Facade.Accepted)
      }
    }

    results.expectMsgType[WorkResult].workId._1 should be(PowerOfNJobDef.jobId)

    for (n <- 2 to 100) {
      frontend ! Work(n.toString, n)
      expectMsg(Facade.Accepted)
    }

    results.within(10.seconds) {
      val ids = results.receiveN(99).map { case WorkResult(workId, _) => workId }
      // nothing lost, and no duplicates
      ids.toVector.map(_.toInt).sorted should be((2 to 100).toVector)
    }

  }

}