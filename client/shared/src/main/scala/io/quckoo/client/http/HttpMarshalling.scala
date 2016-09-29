package io.quckoo.client.http

import upickle.default.{Reader => UReader, Writer => UWriter}
import io.quckoo.api.RequestTimeoutHeader
import io.quckoo.auth.Passport
import io.quckoo.client.core._
import io.quckoo.serialization.DataBuffer
import io.quckoo.util._

import scala.concurrent.duration.Duration
import scala.collection.mutable

import scalaz.{Kleisli, \/, Validation}
import scalaz.syntax.either._
import scalaz.syntax.validation._

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
      headerMap += authHeader(pass)
    }
    headerMap.toMap
  }

  protected def marshallEmpty[O <: CmdMarshalling[HttpProtocol]](
    method: HttpMethod, uriFor: O#Cmd[O#In] => String
  ) = Marshall[O#Cmd, O#In, HttpRequest] { cmd =>
    def createRequest(passport: Option[Passport]) = {
      val headers = httpHeaders(passport, cmd.timeout)
      LawfulTry.success(HttpRequest(method, uriFor(cmd), cmd.timeout, headers))
    }

    cmd match {
      case AuthCmd(_, _, passport) => createRequest(Some(passport))
      case AnonCmd(_, _)           => createRequest(None)
    }
  }

  protected def marshallToJson[O <: CmdMarshalling[HttpProtocol]](
    method: HttpMethod, uriFor: O#Cmd[O#In] => String)(
    implicit encoder: UWriter[O#In]
  ): Marshall[O#Cmd, O#In, HttpRequest] = {
    val encodePayload = Kleisli[LawfulTry, O#Cmd[O#In], DataBuffer] { cmd =>
      cmd.payload match {
        case ()  => LawfulTry.success(DataBuffer.Empty)
        case any => DataBuffer(any.asInstanceOf[O#In])
      }
    }

    for {
      entityData  <- encodePayload
      httpRequest <- marshallEmpty[O](method, uriFor)
    } yield httpRequest.copy(entity = entityData, headers = httpRequest.headers + JsonContentTypeHeader)
  }

  protected def unmarshallFromJson[O <: CmdMarshalling[HttpProtocol]](
      implicit decoder: UReader[O#Rslt]
  ): Unmarshall[HttpResponse, O#Rslt] = Unmarshall { res =>
    if (res.isFailure && res.entity.isEmpty) {
      LawfulTry.fail(HttpErrorException(res.statusLine))
    }
    else res.entity.as[O#Rslt](decoder)
  }

  protected def unmarshalOption[A](
    implicit decoder: UReader[A]
  ): Unmarshall[HttpResponse, Option[A]] = Unmarshall { res =>
    if (res.isSuccess) res.entity.as[A].map(Some(_))
    else LawfulTry.success(None)
  }

  protected def unmarshalEither[E, A](
    implicit
    errDecode: UReader[E], succDecode: UReader[A]
  ): Unmarshall[HttpResponse, E \/ A] = Unmarshall { res =>
    if (res.isFailure) res.entity.as[E].map(_.left[A])
    else res.entity.as[A].map(_.right[E])
  }

  protected def unmarshalValidation[E, A](
    implicit
    errDecode: UReader[E], succDecode: UReader[A]
  ): Unmarshall[HttpResponse, Validation[E, A]] = Unmarshall { res =>
    if (res.isFailure) res.entity.as[E].map(_.failure[A])
    else res.entity.as[A].map(_.success[E])
  }

}
