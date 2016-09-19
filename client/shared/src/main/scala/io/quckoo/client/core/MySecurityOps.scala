package io.quckoo.client.core

import io.quckoo.auth.{Credentials, Passport}

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait MySecurityOps[P <: Protocol] {
  type AuthenticateOp = ProtocolOp.AnonOp[P, Credentials, Passport]
  type SingOutOp = ProtocolOp.AuthOp[P, Unit, Unit]

  implicit def authenticateOp: AuthenticateOp
  implicit def signOutOp: SingOutOp
}
