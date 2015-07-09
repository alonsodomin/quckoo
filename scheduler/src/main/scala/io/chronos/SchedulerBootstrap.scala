package io.chronos

import java.time.Clock

import akka.actor._
import akka.contrib.pattern.ClusterSingletonManager
import akka.pattern.ask
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import io.chronos.scheduler.Scheduler

import scala.concurrent.duration._

/**
 * Created by domingueza on 09/07/15.
 */
object SchedulerBootstrap extends App {

  startBackend(2551, "backend")
  startBackend(2552, "backend")

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
        Scheduler.props(clock), "active", PoisonPill, Some(role)
      ),
      "scheduler"
    )
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
