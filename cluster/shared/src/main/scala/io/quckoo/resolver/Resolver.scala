package io.quckoo.resolver

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern._

import io.quckoo.fault.ResolutionFault
import io.quckoo.id.ArtifactId

import scalaz._

/**
  * Created by alonsodomin on 11/04/2016.
  */
object Resolver {

  final case class Validate(artifactId: ArtifactId)
  final case class Download(artifactId: ArtifactId)
  final case class ArtifactResolved(artifact: Artifact)
  final case class ResolutionFailed(cause: NonEmptyList[ResolutionFault])

  def props(resolve: Resolve): Props =
    Props(classOf[Resolver], resolve)

}

class Resolver(resolve: Resolve) extends Actor with ActorLogging {
  import Resolver._
  import context.dispatcher

  def receive = {
    case Validate(artifactId) =>
      log.debug("Validating artifact {}", artifactId)
      resolve(artifactId, download = false) map {
        case Success(artifact) => ArtifactResolved(artifact)
        case Failure(cause)    => ResolutionFailed(cause)
      } pipeTo sender()

    case Download(artifactId) =>
      log.debug("Downloading artifact {}", artifactId)
      resolve(artifactId, download = true) map {
        case Success(artifact) => ArtifactResolved(artifact)
        case Failure(cause)    => ResolutionFailed(cause)
      } pipeTo sender()
  }

}
