package io.quckoo.cluster.registry

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.pattern._
import io.quckoo.{JobId, JobNotFound, JobSpec}
import io.quckoo.api2.{Registry => RegistryAPI}
import io.quckoo.auth.{Passport, Session}
import io.quckoo.protocol.registry.GetJob

import scala.concurrent.Future
import scala.concurrent.duration._

class RegistryModule(core: ActorRef)(implicit actorSystem: ActorSystem) extends RegistryAPI[Future] {

  override def enableJob(session: Session.Authenticated)(jobId: JobId) = ???

  override def disableJob(session: Session.Authenticated)(jobId: JobId) = ???

  override def fetchJob(session: Session.Authenticated)(jobId: JobId): Future[Option[JobSpec]] = {
    import actorSystem.dispatcher
    implicit val timeout = Timeout(5 seconds)

    (core ? GetJob(jobId)).map {
      case JobNotFound(_) => None
      case spec: JobSpec  => Some(spec)
    }
  }

  override def registerJob(session: Session.Authenticated)(jobSpec: JobSpec) = ???
}
