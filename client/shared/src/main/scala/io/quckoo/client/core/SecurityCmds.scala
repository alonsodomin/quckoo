package io.quckoo.client.core

import io.quckoo.auth.{Credentials, Passport}

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait SecurityCmds[P <: Protocol] {
  type AuthenticateOp = CmdMarshalling.Anon[P, Credentials, Passport]
  type SingOutOp      = CmdMarshalling.Auth[P, Unit, Unit]

  implicit def authenticateOp: AuthenticateOp
  implicit def signOutOp: SingOutOp
}
