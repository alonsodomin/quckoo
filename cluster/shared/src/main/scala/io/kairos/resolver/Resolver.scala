package io.kairos.resolver

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.pattern._
import io.kairos.id.ArtifactId
import io.kairos.protocol.ExceptionThrown

import scala.concurrent._
import scalaz.Scalaz._

/**
 * Created by aalonsodominguez on 22/08/15.
 */
object Resolver {

  def props(resolve: Resolve) = Props(classOf[Resolver], resolve)

  case class Validate(artifactId: ArtifactId)
  case class Acquire(artifactId: ArtifactId)

}

class Resolver(resolve: Resolve) extends Actor with ActorLogging {

  import Resolver._

  def receive: Receive = {
    case Validate(artifactId) =>
      resolution(artifactId, download = false, sender())

    case Acquire(artifactId) =>
      resolution(artifactId, download = true, sender())
  }

  private[this] def resolution(artifactId: ArtifactId, download: Boolean, requestor: ActorRef): Unit = {
    import context.dispatcher
    Future { resolve(artifactId, download) } recover {
      case t: Throwable => ExceptionThrown(t).failureNel[Artifact]
    } pipeTo requestor
  }

}
