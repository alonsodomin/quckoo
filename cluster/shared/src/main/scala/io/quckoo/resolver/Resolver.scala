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

package io.quckoo.resolver

import akka.actor.{Actor, ActorLogging, Props}
import akka.pattern._
import io.quckoo.fault.Fault
import io.quckoo.id.ArtifactId

import scalaz._

/**
  * Created by alonsodomin on 11/04/2016.
  */
object Resolver {

  final case class Validate(artifactId: ArtifactId)
  final case class Download(artifactId: ArtifactId)
  final case class ArtifactResolved(artifact: Artifact)
  final case class ResolutionFailed(artifactId: ArtifactId, cause: Fault)

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
        case Failure(cause)    => ResolutionFailed(artifactId, cause)
      } pipeTo sender()

    case Download(artifactId) =>
      log.debug("Downloading artifact {}", artifactId)
      resolve(artifactId, download = true) map {
        case Success(artifact) => ArtifactResolved(artifact)
        case Failure(cause)    => ResolutionFailed(artifactId, cause)
      } pipeTo sender()
  }

}
