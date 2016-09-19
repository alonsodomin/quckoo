package io.quckoo.client

import io.quckoo.util.LawfulTry

import scalaz.Kleisli

/**
  * Created by alonsodomin on 10/09/2016.
  */
package object core {
  type Marshall[Cmd[_] <: Command[_], In, Req] = Kleisli[LawfulTry, Cmd[In], Req]
  object Marshall {
    def apply[Cmd[_] <: Command[_], In, Req](run: Cmd[In] => LawfulTry[Req]): Marshall[Cmd, In, Req] =
      Kleisli[LawfulTry, Cmd[In], Req](run)
  }

  type Unmarshall[Res, Rslt] = Kleisli[LawfulTry, Res, Rslt]
  object Unmarshall {
    def apply[Res, Rslt](run: Res => LawfulTry[Rslt]): Unmarshall[Res, Rslt] =
      Kleisli[LawfulTry, Res, Rslt](run)
  }
}
