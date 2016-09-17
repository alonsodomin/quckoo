package io.quckoo.client

import io.quckoo.util.TryE

/**
  * Created by alonsodomin on 10/09/2016.
  */
package object core {
  type Marshall[Cmd[_] <: Command[_], In, Req] = Cmd[In] => TryE[Req]
  type Unmarshall[Res, Rslt] = Res => TryE[Rslt]

  type Recover[Rslt] = PartialFunction[Throwable, Rslt]
  object Recover {
    def noRecover[Rslt]: Recover[Rslt] = PartialFunction.empty[Throwable, Rslt]
  }
}
