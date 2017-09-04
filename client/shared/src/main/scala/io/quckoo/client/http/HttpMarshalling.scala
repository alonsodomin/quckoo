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

import cats.data.{Kleisli, Validated}
import cats.instances.either._
import cats.syntax.validated._

import io.circe.Decoder._

import io.quckoo.api.RequestTimeoutHeader
import io.quckoo.auth.Passport
import io.quckoo.client.core._
import io.quckoo.serialization.{DataBuffer, Encoder, Decoder}
import io.quckoo.serialization.json._
import io.quckoo.util._

import scala.concurrent.duration.Duration
import scala.collection.mutable

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait HttpMarshalling {

  protected def httpHeaders(passport: Option[Passport], timeout: Duration): Map[String, String] = {
    val headerMap = mutable.Map.empty[String, String]
    if (timeout.isFinite()) {
      headerMap += (RequestTimeoutHeader -> timeout.toMillis.toString)
    }
    passport.foreach { pass =>
      headerMap += bearerToken(pass)
    }
    headerMap.toMap
  }

  protected def marshallEmpty[O <: CmdMarshalling[HttpProtocol]](
                                                                  method: HttpMethod1,
                                                                  uriFor: O#Cmd[O#In] => String
  ) = Marshall[O#Cmd, O#In, HttpRequest1] { cmd =>
    def createRequest(passport: Option[Passport]) = {
      val headers = httpHeaders(passport, cmd.timeout)
      Attempt.success(HttpRequest1(method, uriFor(cmd), cmd.timeout, headers))
    }

    cmd match {
      case AuthCmd(_, _, passport) => createRequest(Some(passport))
      case AnonCmd(_, _)           => createRequest(None)
    }
  }

  protected def marshallToJson[O <: CmdMarshalling[HttpProtocol]](method: HttpMethod1,
                                                                  uriFor: O#Cmd[O#In] => String)(
      implicit encoder: Encoder[O#In, String]
  ): Marshall[O#Cmd, O#In, HttpRequest1] = {
    val encodePayload = Kleisli[Attempt, O#Cmd[O#In], DataBuffer] { cmd =>
      cmd.payload match {
        case ()  => Attempt.success(DataBuffer.Empty)
        case any => DataBuffer(any.asInstanceOf[O#In])
      }
    }

    for {
      entityData  <- encodePayload
      httpRequest <- marshallEmpty[O](method, uriFor)
    } yield
      httpRequest.copy(entity = entityData, headers = httpRequest.headers + JsonContentTypeHeader)
  }

  protected def unmarshallFromJson[O <: CmdMarshalling[HttpProtocol]](
      implicit decoder: Decoder[String, O#Rslt]
  ): Unmarshall[HttpResponse1, O#Rslt] = Unmarshall { res =>
    if (res.isFailure && res.entity.isEmpty) {
      Attempt.fail(HttpError(res.statusLine))
    } else res.entity.as[O#Rslt](decoder)
  }

  protected def unmarshalOption[A](
      implicit decoder: Decoder[String, A]
  ): Unmarshall[HttpResponse1, Option[A]] = Unmarshall { res =>
    if (res.isSuccess) res.entity.as[A].map(Some(_))
    else Attempt.success(None)
  }

  protected def unmarshalEither[E, A](
      implicit errDecode: Decoder[String, E],
      succDecode: Decoder[String, A]
  ): Unmarshall[HttpResponse1, Either[E, A]] = Unmarshall { res =>
    if (res.isFailure) res.entity.as[E].map(Left(_))
    else res.entity.as[A].map(Right(_))
  }

  protected def unmarshalValidation[E, A](
      implicit errDecode: Decoder[String, E],
      succDecode: Decoder[String, A]
  ): Unmarshall[HttpResponse1, Validated[E, A]] = Unmarshall { res =>
    if (res.isFailure) res.entity.as[E].map(_.invalid[A])
    else res.entity.as[A].map(_.valid[E])
  }

}
