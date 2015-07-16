package io.chronos

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import io.chronos.worker.{ChronosRepositorySystem, JobExecutor, ModuleResolver, Worker}
import org.codehaus.plexus.classworlds.ClassWorld

/**
 * Created by domingueza on 09/07/15.
 */
object WorkerBootstrap extends App {

  val defaultPort = 0

  val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + defaultPort)
    .withFallback(ConfigFactory.load("worker"))
  val system = ActorSystem("WorkerSystem", conf)
  val initialContacts = immutableSeq(conf.getStringList("contact-points")).map {
    case AddressFromURIString(addr) => system.actorSelection(RootActorPath(addr) / "user" / "receptionist")
  }.toSet

  val classWorld = new ClassWorld()

  val repositorySystem = ChronosRepositorySystem.repositorySystem
  val repositorySession = ChronosRepositorySystem.repositorySession(repositorySystem)
  val moduleResolver = new ModuleResolver(repositorySystem, repositorySession)

  val clusterClient = system.actorOf(ClusterClient.props(initialContacts), "clusterClient")
  system.actorOf(Worker.props(clusterClient, JobExecutor.props(classWorld, moduleResolver)), "worker")

}
