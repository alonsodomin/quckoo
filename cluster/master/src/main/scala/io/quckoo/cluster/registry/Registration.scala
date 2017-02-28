/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.registry

import java.util.concurrent.TimeoutException

import akka.actor.{Actor, ActorLogging, ActorRef, Props, ReceiveTimeout, Stash}
import akka.cluster.pubsub.{DistributedPubSub, DistributedPubSubMediator}

import io.quckoo._
import io.quckoo.api.Topic
import io.quckoo.protocol.registry.{JobAccepted, JobRejected}
import io.quckoo.resolver.Resolver

import scala.concurrent.duration._

object Registration {

  def props(jobSpec: JobSpec, shardsGuardian: ActorRef, resolver: ActorRef, replyTo: ActorRef): Props = {
    jobSpec.jobPackage match {
      case JarJobPackage(artifactId, _) =>
        Props(new JarRegistration(jobSpec, artifactId, shardsGuardian, resolver, replyTo))

      case _ =>
        Props(new SimpleRegistration(jobSpec, shardsGuardian, replyTo))
    }
  }

}

abstract class Registration private[registry] (jobSpec: JobSpec, replyTo: ActorRef)
  extends Actor with ActorLogging with Stash {

  private[this] val mediator = DistributedPubSub(context.system).mediator

  val jobId = JobId(jobSpec)

  override def preStart(): Unit =
    mediator ! DistributedPubSubMediator.Subscribe(Topic.Registry.name, self)

  final def receive: Receive = initializing

  private def initializing: Receive = {
    case DistributedPubSubMediator.SubscribeAck(_) =>
      unstashAll()
      context become startRegistration

    case _ => stash()
  }

  def startRegistration: Receive

  protected def awaitAcceptance: Receive = {
    case evt @ JobAccepted(`jobId`, _) =>
      replyTo ! evt
      finish()

    case ReceiveTimeout =>
      log.error("Timed out whilst awaiting for the job '{}' to be accepted.", jobId)
      replyTo ! JobRejected(jobId, ExceptionThrown.from(new TimeoutException))
      finish()
  }

  private def stopping: Receive = {
    case DistributedPubSubMediator.UnsubscribeAck(_) =>
      context stop self
  }

  protected def finish(): Unit = {
    mediator ! DistributedPubSubMediator.Unsubscribe(topics.Registry, self)
    context become stopping
  }

}

class SimpleRegistration private[registry] (jobSpec: JobSpec, shardsGuardian: ActorRef, replyTo: ActorRef)
    extends Registration(jobSpec, replyTo) {

  override def startRegistration: Receive = {
    shardsGuardian ! PersistentJob.CreateJob(jobId, jobSpec)
    awaitAcceptance
  }

}

class JarRegistration private[registry] (
    jobSpec: JobSpec, artifactId: ArtifactId, shardsGuardian: ActorRef, resolver: ActorRef, replyTo: ActorRef
  ) extends Registration(jobSpec, replyTo) {
  import Resolver._

  override def startRegistration: Receive = {
    resolver ! Resolver.Validate(artifactId)
    resolvingArtifact
  }

  def resolvingArtifact: Receive = {
    case ArtifactResolved(artifact) =>
      log.debug("Job artifact has been successfully resolved. artifactId={}", artifact.artifactId)
      shardsGuardian ! PersistentJob.CreateJob(jobId, jobSpec)
      context.setReceiveTimeout(10 seconds)
      context become awaitAcceptance

    case ResolutionFailed(_, cause) =>
      log.error("Couldn't validate the job artifact id. " + cause)
      replyTo ! JobRejected(jobId, cause)
      finish()
  }

}