package io.quckoo.client

import io.quckoo.util.LawfulTry

/**
  * Created by alonsodomin on 10/09/2016.
  */
package object core {
  type Marshall[Cmd[_] <: Command[_], In, Req] = Cmd[In] => LawfulTry[Req]
  type Unmarshall[Res, Rslt] = Res => LawfulTry[Rslt]
}
