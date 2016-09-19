package io.quckoo.client.http

import io.quckoo.auth.{Credentials, InvalidCredentialsException, Passport}
import io.quckoo.client.core._
import io.quckoo.serialization.DataBuffer
import io.quckoo.util.LawfulTry

/**
  * Created by alonsodomin on 19/09/2016.
  */
object MyHttpSecurityOps {
  import ProtocolOp._

  final val AuthenticateOp = new AnonOp[HttpProtocol, Credentials, Passport] {

    override val marshall = Marshall[AnonCmd, Credentials, HttpRequest] { cmd =>
      val creds = DataBuffer.fromString(s"${cmd.payload.username}:${cmd.payload.password}").toBase64
      val hdrs = Map(AuthorizationHeader -> s"Basic $creds")

      LawfulTry.success(HttpRequest(HttpMethod.Post, LoginURI, cmd.timeout, headers = hdrs))
    }

    override val unmarshall = Unmarshall[HttpResponse, Passport] { res =>
      if (res.isSuccess) Passport(res.entity.asString())
      else LawfulTry.failed {
        if (res.statusCode == 401) InvalidCredentialsException
        else HttpErrorException(res.statusLine)
      }
    }
  }

  final val SingOutOp = new AuthOp[HttpProtocol, Unit, Unit] {
    override val marshall = Marshall[AuthCmd, Unit, HttpRequest] { cmd =>
      LawfulTry.success {
        HttpRequest(HttpMethod.Post, LogoutURI, cmd.timeout, Map(authHeader(cmd.passport)))
      }
    }

    override val unmarshall = Unmarshall[HttpResponse, Unit] { res =>
      if (res.isSuccess) LawfulTry.unit
      else LawfulTry.failed(HttpErrorException(res.statusLine))
    }
  }

}

trait MyHttpSecurityOps extends MySecurityOps[HttpProtocol] {
  override implicit def authenticateOp = MyHttpSecurityOps.AuthenticateOp
  override implicit def signOutOp = MyHttpSecurityOps.SingOutOp
}
