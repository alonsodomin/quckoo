package io.chronos

import java.time.Clock

import akka.actor._
import akka.contrib.pattern.{ClusterClient, ClusterSingletonManager}
import akka.japi.Util._
import akka.pattern.ask
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.chronos.example.PowerOfNActor
import io.chronos.receptor.ReceptorActor
import io.chronos.scheduler._
import io.chronos.worker.{JobExecutor, Worker}

import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 05/07/15.
 */
object Main {

  def main(args: Array[String]): Unit = {
    if (args.isEmpty) {
      startBackend(2551, "backend")
      Thread.sleep(5000)
      startBackend(2552, "backend")
      startWorker(0)
      Thread.sleep(5000)
      startFrontend(0)
    } else {
      val port = args(0).toInt
      if (2000 <= port && port <= 2999)
        startBackend(port, "backend")
      else if (3000 <= port && port <= 3999)
        startFrontend(port)
      else
        startWorker(port)
    }
  }

  def workTimeout = 10.seconds

  def startBackend(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]")
      .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)
    val clock = Clock.systemUTC()

    startupSharedJournal(system, startStore = (port == 2551), path =
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))

    system.actorOf(
      ClusterSingletonManager.props(
        Scheduler.props(clock, workTimeout), "active", PoisonPill, Some(role)
      ),
      "scheduler"
    )
  }

  def startFrontend(port: Int): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)
    val frontend = system.actorOf(Props[ReceptorActor], "receptor")
    system.actorOf(PowerOfNActor.props(frontend), "producer")
    system.actorOf(Props[WorkResultConsumer], "consumer")
  }

  def startWorker(port: Int): Unit = {
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port)
      .withFallback(ConfigFactory.load("worker"))
    val system = ActorSystem("WorkerSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    system.actorOf(Worker.props(clusterClient, Props[JobExecutor]), "worker")
  }

  def startupSharedJournal(system: ActorSystem, startStore: Boolean, path: ActorPath): Unit = {
    // Start the shared journal one one node (don't crash this SPOF)
    // This will not be needed with a distributed journal
    if (startStore) {
      system.actorOf(Props[SharedLeveldbStore], "store")
    }

    // register the shared journal
    import system.dispatcher
    implicit val timeout = Timeout(15.seconds)
    val f = system.actorSelection(path) ? Identify(None)

    f.onSuccess {
      case ActorIdentity(_, Some(ref)) => SharedLeveldbJournal.setStore(ref, system)
      case _ =>
        system.log.error("Shared journal not started at {}", path)
        system.shutdown()
    }
    f.onFailure {
      case _ =>
        system.log.error("Lookup of shared journal at {} timed out", path)
        system.shutdown()
    }
  }

}
