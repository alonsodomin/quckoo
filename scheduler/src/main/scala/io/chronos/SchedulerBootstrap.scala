package io.chronos

import java.time.Clock

import akka.actor._
import akka.contrib.pattern.ClusterClient
import akka.japi.Util._
import akka.pattern.ask
import akka.persistence.journal.leveldb.{SharedLeveldbJournal, SharedLeveldbStore}
import akka.util.Timeout
import com.hazelcast.core.Hazelcast
import com.typesafe.config.ConfigFactory
import io.chronos.example.PowerOfNActor
import io.chronos.scheduler.{HazelcastJobRegistry, Repository, Scheduler}

import scala.concurrent.duration._

/**
 * Created by domingueza on 09/07/15.
 */
object SchedulerBootstrap extends App {

  val hazelcastInstance = Hazelcast.newHazelcastInstance()
  val jobRegistry = new HazelcastJobRegistry(hazelcastInstance)

  startScheduler(2551, "scheduler")
  startScheduler(2552, "scheduler")
  startFrontend(0)

  def startScheduler(port: Int, role: String): Unit = {
    val conf = ConfigFactory.parseString(s"akka.cluster.roles=[$role]")
      .withFallback(ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port))
      .withFallback(ConfigFactory.load())
    val system = ActorSystem("ClusterSystem", conf)
    val clock = Clock.systemUTC()

    /*startupSharedJournal(system, startStore = (port == 2551), path =
      ActorPath.fromString("akka.tcp://ClusterSystem@127.0.0.1:2551/user/store"))*/

    system.actorOf(Scheduler.props(clock, jobRegistry), "scheduler")
    system.actorOf(Repository.props(jobRegistry), "repository")

    system.actorOf(Props[WorkResultConsumer], "consumer")
  }

  def startFrontend(port: Int): Unit = {
    val defaultPort = 0

    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + defaultPort)
      .withFallback(ConfigFactory.load("facade"))
    val system = ActorSystem("FacadeSystem", conf)
    val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
      case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
    val frontend = system.actorOf(Facade.props(clusterClient), "frontend")
    system.actorOf(Props(classOf[PowerOfNActor], frontend), "producer")
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
