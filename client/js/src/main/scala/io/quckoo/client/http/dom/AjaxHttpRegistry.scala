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

package io.quckoo.client.http.dom

import cats.Eval
import cats.data.ValidatedNel
import cats.effect.IO
import cats.implicits._

import io.circe.parser.decode
import io.circe.generic.auto._
import io.circe.syntax._

import io.quckoo.{JobId, JobNotFound, JobSpec, QuckooError}
import io.quckoo.api2.Registry
import io.quckoo.client.ClientIO
import io.quckoo.client.http._
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled}

import org.scalajs.dom.ext.Ajax
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

trait AjaxHttpRegistry extends Registry[ClientIO] {

  private def jobAction[A](jobId: JobId, action: String)(
      onSuccess: JobId => A
  ): ClientIO[Either[JobNotFound, A]] =
    ClientIO.auth { session =>
      val uri = s"$JobsURI/$jobId/$action"
      val ajax = IO.fromFuture(Eval.later {
        Ajax.post(uri, headers = Map(bearerToken(session.passport)))
      })

      ajax >>= handleResponse {
        case res if (res.status == 200) || (res.status == 204) =>
          IO.pure(Right(onSuccess(jobId)))
        case res if res.status == 404 =>
          IO.pure(Left(JobNotFound(jobId)))
      }
    }

  override def enableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobEnabled]] =
    jobAction(jobId, "enable")(JobEnabled)

  override def disableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobDisabled]] =
    jobAction(jobId, "disabled")(JobDisabled)

  override def allJobs: ClientIO[Seq[(JobId, JobSpec)]] = ClientIO.auth { session =>
    val ajax = IO.fromFuture(Eval.later {
      Ajax.get(JobsURI, headers = Map(bearerToken(session.passport)))
    })

    ajax >>= handleResponse {
      case res if res.status == 200 =>
        IO.async[Seq[(JobId, JobSpec)]](_(decode[Seq[(JobId, JobSpec)]](res.responseText)))
    }
  }

  override def fetchJob(jobId: JobId): ClientIO[Option[JobSpec]] =
    ClientIO.auth { session =>
      val uri = s"$JobsURI/$jobId"
      val ajax = IO.fromFuture(Eval.later {
        Ajax.get(uri, headers = Map(bearerToken(session.passport)))
      })

      ajax >>= handleResponse {
        case res if res.status == 200 =>
          IO.async[Option[JobSpec]](_(decode[JobSpec](res.responseText).map(Some(_))))
        case res if res.status == 404 =>
          IO.pure(None)
      }
    }

  override def registerJob(jobSpec: JobSpec): ClientIO[ValidatedNel[QuckooError, JobId]] =
    ClientIO.auth { session =>
      val ajax = IO.fromFuture(Eval.later {
        Ajax.put(
          JobsURI,
          data = jobSpec.asJson.noSpaces,
          headers = Map(bearerToken(session.passport))
        )
      })

      ajax >>= handleResponse {
        case res if res.status == 200 =>
          IO.async[ValidatedNel[QuckooError, JobId]](
            _(decode[ValidatedNel[QuckooError, JobId]](res.responseText))
          )
      }
    }
}
