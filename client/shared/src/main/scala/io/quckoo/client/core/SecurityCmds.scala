package io.quckoo.client.core

import io.quckoo.auth.{Credentials, Passport}

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait SecurityCmds[P <: Protocol] {
  type AuthenticateCmd = CmdMarshalling.Anon[P, Credentials, Passport]
  type RefreshTokenCmd = CmdMarshalling.Auth[P, Unit, Passport]
  type SingOutCmd      = CmdMarshalling.Auth[P, Unit, Unit]

  implicit def authenticateCmd: AuthenticateCmd
  implicit def refreshTokenCmd: RefreshTokenCmd
  implicit def signOutCmd: SingOutCmd
}
