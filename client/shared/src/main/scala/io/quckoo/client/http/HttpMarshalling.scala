package io.quckoo.client.http

import upickle.default.{Reader => UReader, Writer => UWriter}

import io.quckoo.auth.Passport
import io.quckoo.client.core._
import io.quckoo.serialization.DataBuffer
import io.quckoo.util._

import scalaz.Kleisli

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait HttpMarshalling {

  protected def marshallEmpty[O <: CmdMarshalling[HttpProtocol]](
    method: HttpMethod, uriFor: O#Cmd[O#In] => String
  ) = Marshall[O#Cmd, O#In, HttpRequest] { cmd =>
    def createRequest(passport: Option[Passport]) = {
      val headers = passport.map(pass => Map(authHeader(pass))).getOrElse(Map.empty[String, String])
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

}
