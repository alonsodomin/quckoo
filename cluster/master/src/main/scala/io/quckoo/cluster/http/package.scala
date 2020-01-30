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

package io.quckoo.cluster

import java.util.UUID

import akka.http.scaladsl.marshalling.{Marshaller, ToEntityMarshaller}
import akka.http.scaladsl.model.{MediaTypes, MediaType, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.stream.scaladsl.Source

import cats.effect.IO
import cats.implicits._

import de.heikoseeberger.akkahttpcirce.{ErrorAccumulatingCirceSupport => JsonSupport}
import de.heikoseeberger.akkasse.scaladsl.model.ServerSentEvent

import io.circe.Encoder

import io.quckoo._
import io.quckoo.api.TopicTag

import play.twirl.api.{Html, Txt, Xml}

import scala.concurrent.duration._
import scala.concurrent.{Future, ExecutionContext}
import scala.language.implicitConversions

/**
  * Created by alonsodomin on 24/03/2016.
  */
package object http extends IORoutes {
  import MediaTypes._

  /** Twirl marshallers for Xml, Html and Txt mediatypes */
  implicit val twirlHtmlMarshaller = twirlMarshaller[Html](`text/html`)
  implicit val twirlTxtMarshaller  = twirlMarshaller[Txt](`text/plain`)
  implicit val twirlXmlMarshaller  = twirlMarshaller[Xml](`text/xml`)

  def twirlMarshaller[A](contentType: MediaType): ToEntityMarshaller[A] =
    Marshaller.StringMarshaller.wrap(contentType)(_.toString)

  implicit def asSSE[A](source: Source[A, _])(
      implicit
      topicTag: TopicTag[A],
      encode: Encoder[A]
  ): Source[ServerSentEvent, _] =
    source
      .map(event => ServerSentEvent(encode(event).noSpaces, topicTag.name))
      .keepAlive(1 second, () => ServerSentEvent.heartbeat)

  def generateAuthToken: String = UUID.randomUUID().toString

}

trait IORoutes {
  import JsonSupport._

  implicit def routeIO[A](action: IO[A])(
      implicit
      ec: ExecutionContext,
      enc: Encoder[A]
  ): Route = {
    val resolveRoute = action.map(res => complete(StatusCodes.OK -> res)).recover {
      case JobNotFound(jobId)            => complete(StatusCodes.NotFound -> jobId)
      case ExecutionPlanNotFound(planId) => complete(StatusCodes.NotFound -> planId)
    }
    onSuccess(resolveRoute.unsafeToFuture())(identity)
  }

}
