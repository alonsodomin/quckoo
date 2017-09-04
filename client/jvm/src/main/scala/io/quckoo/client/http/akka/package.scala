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

package io.quckoo.client.http

import _root_.akka.http.scaladsl.Http.OutgoingConnection
import _root_.akka.http.scaladsl.model.{RequestEntity, HttpRequest, HttpResponse, StatusCodes}
import _root_.akka.http.scaladsl.model.headers.{Authorization, OAuth2BearerToken}
import _root_.akka.http.scaladsl.marshalling.{ToEntityMarshaller, Marshal}
import _root_.akka.http.scaladsl.unmarshalling.{FromEntityUnmarshaller, Unmarshal}
import _root_.akka.stream.Materializer
import _root_.akka.stream.scaladsl.Flow

import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport

import cats.Eval
import cats.effect.IO
import io.circe.{Decoder, Encoder}
import io.circe.parser.decode

import io.quckoo.auth.{NotAuthorized, Session, SessionExpired}
import io.quckoo.util.attempt2Future

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration.FiniteDuration

package object akka {

  type HttpClient             = Flow[HttpRequest, HttpResponse, Future[OutgoingConnection]]
  type HttpResponseHandler[A] = PartialFunction[HttpResponse, IO[A]]
  type HttpResponseCheck      = HttpResponse => Boolean

  private final val ParsableHttpResponse: HttpResponseCheck = _.status.allowsEntity()

  implicit class RichHttpRequest(request: HttpRequest) {

    def withSession(session: Session): HttpRequest = session match {
      case Session.Anonymous => request
      case Session.Authenticated(passport) =>
        val bearerToken = OAuth2BearerToken(passport.token)
        request.withHeaders(Authorization(bearerToken))
    }

  }

  def marshalEntity[A](payload: A)(
      implicit
      ec: ExecutionContext,
      marshaller: ToEntityMarshaller[A]
  ): IO[RequestEntity] =
    IO.fromFuture(Eval.later(Marshal(payload).to[RequestEntity]))

  def unmarshalEntity[A](response: HttpResponse)(
      implicit
      ec: ExecutionContext,
      materializer: Materializer,
      unmarshaller: FromEntityUnmarshaller[A]
  ): IO[A] =
    IO.fromFuture(Eval.later(Unmarshal(response).to[A]))

  def handleEntity[A](check: HttpResponseCheck = ParsableHttpResponse)(
      implicit
      executionContext: ExecutionContext,
      materializer: Materializer,
      unmarshaller: FromEntityUnmarshaller[A]
  ): HttpResponseHandler[A] = {
    case response if check(response) =>
      unmarshalEntity(response)
  }

  def handleResponse[A](handler: HttpResponseHandler[A]): HttpResponse => IO[A] = {
    def defaultHandler: HttpResponse => IO[A] =
      res =>
        res.status match {
          case StatusCodes.Unauthorized =>
            IO.raiseError(SessionExpired)
          case StatusCodes.Forbidden =>
            IO.raiseError(NotAuthorized)
          case error: StatusCodes.ClientError =>
            IO.raiseError(new Exception(error.defaultMessage))
          case _ => IO.raiseError(new Exception(res.status.defaultMessage()))
      }

    handler.applyOrElse(_, defaultHandler)
  }

}
