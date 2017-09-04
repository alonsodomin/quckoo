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
import cats.effect.IO
import cats.implicits._

import io.circe.parser.decode
import io.circe.generic.auto._

import io.quckoo.{JobId, JobNotFound, JobSpec}
import io.quckoo.api2.Registry
import io.quckoo.client.ClientIO
import io.quckoo.client.http._
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled}

import org.scalajs.dom.ext.Ajax
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

trait AjaxHttpRegistry extends Registry[ClientIO] {

  override def enableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobEnabled]] =
    ClientIO.auth { session =>
      val uri = s"$JobsURI/$jobId/enable"
      val ajax = IO.fromFuture(Eval.later {
        Ajax.post(uri, headers = Map(bearerToken(session.passport)))
      })

      ajax >>= handleResponse {
        case res if (res.status == 200) || (res.status == 204) =>
          IO.pure(Right(JobEnabled(jobId)))
        case res if res.status == 404 =>
          IO.pure(Left(JobNotFound(jobId)))
      }
    }

  override def disableJob(jobId: JobId): ClientIO[Either[JobNotFound, JobDisabled]] =
    ClientIO.auth { session =>
      val uri = s"$JobsURI/$jobId/disable"
      val ajax = IO.fromFuture(Eval.later {
        Ajax.post(uri, headers = Map(bearerToken(session.passport)))
      })

      ajax >>= handleResponse {
        case res if (res.status == 200) || (res.status == 204) =>
          IO.pure(Right(JobDisabled(jobId)))
        case res if res.status == 404 =>
          IO.pure(Left(JobNotFound(jobId)))
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
          IO.async[Option[JobSpec]](cb => cb(decode[JobSpec](res.responseText).map(Some(_))))
        case res if res.status == 404 =>
          IO.pure(None)
      }
    }

  override def registerJob(jobSpec: JobSpec) = ???
}
