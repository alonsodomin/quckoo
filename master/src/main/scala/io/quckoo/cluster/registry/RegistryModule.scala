/*
 * Copyright 2015 A. Alonso Dominguez
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

import akka.actor.{ActorRef, ActorSystem}
import akka.util.Timeout
import akka.pattern._
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, OverflowStrategy}
import akka.stream.scaladsl.Source

import cats.Eval
import cats.data.{EitherT, ValidatedNel}
import cats.effect.{LiftIO, IO}
import cats.implicits._

import io.quckoo.api2.{QuckooIO, Registry => RegistryAPI}
import io.quckoo.cluster.QuckooFacade.DefaultBufferSize
import io.quckoo.protocol.registry._
import io.quckoo._

import scala.concurrent.Future
import scala.concurrent.duration._

class RegistryModule(registryActor: ActorRef)(implicit actorSystem: ActorSystem)
    extends RegistryAPI[QuckooIO] {
  import actorSystem.dispatcher

  implicit val materialier: ActorMaterializer = ActorMaterializer(
    ActorMaterializerSettings(actorSystem),
    "registry"
  )

  override def enableJob(jobId: JobId): QuckooIO[Either[JobNotFound, JobEnabled]] = {
    val action = IO.fromFuture(Eval.later {
      implicit val timeout = Timeout(5 seconds)
      (registryActor ? EnableJob(jobId)).map {
        case msg: JobNotFound => msg.asLeft[JobEnabled]
        case msg: JobEnabled  => msg.asRight[JobNotFound]
      }
    })

    LiftIO[QuckooIO].liftIO(action)
  }

  override def disableJob(jobId: JobId): QuckooIO[Either[JobNotFound, JobDisabled]] = {
    val action = IO.fromFuture(Eval.later {
      implicit val timeout = Timeout(5 seconds)
      (registryActor ? EnableJob(jobId)).map {
        case msg: JobNotFound => msg.asLeft[JobDisabled]
        case msg: JobDisabled => msg.asRight[JobNotFound]
      }
    })

    LiftIO[QuckooIO].liftIO(action)
  }

  override def fetchJob(jobId: JobId): QuckooIO[Option[JobSpec]] = {
    val action = IO.fromFuture(Eval.later {
      implicit val timeout = Timeout(5 seconds)
      (registryActor ? GetJob(jobId)).map {
        case JobNotFound(_) => None
        case spec: JobSpec  => Some(spec)
      }
    })

    LiftIO[QuckooIO].liftIO(action)
  }

  override def allJobs: QuckooIO[Seq[(JobId, JobSpec)]] = {
    val action = IO.fromFuture(Eval.later {
      Source
        .actorRef[(JobId, JobSpec)](bufferSize = DefaultBufferSize, OverflowStrategy.fail)
        .mapMaterializedValue { upstream =>
          registryActor.tell(GetJobs, upstream)
        }
        .runFold(Map.empty[JobId, JobSpec])(_ + _)
        .map(_.toList)
    })

    LiftIO[QuckooIO].liftIO(action)
  }

  override def registerJob(jobSpec: JobSpec): QuckooIO[ValidatedNel[QuckooError, JobId]] = {
    val action = IO.fromFuture(Eval.later {
      val validatedJobSpec = JobSpec.valid.async
        .run(jobSpec)
        .map(_.leftMap(ValidationFault).leftMap(_.asInstanceOf[QuckooError]))

      EitherT(validatedJobSpec.map(_.toEither))
        .flatMapF { validJobSpec =>
          implicit val timeout = Timeout(10 minutes)

          (registryActor ? RegisterJob(validJobSpec)) map {
            case JobAccepted(jobId, _) => jobId.asRight[QuckooError]
            case JobRejected(_, error) => error.asLeft[JobId]
          }
        }
        .value
        .map(_.toValidatedNel)
    })

    LiftIO[QuckooIO].liftIO(action)
  }
}
