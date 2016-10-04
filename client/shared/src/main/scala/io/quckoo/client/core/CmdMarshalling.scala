/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.client.core

/**
  * Created by alonsodomin on 18/09/2016.
  */
trait CmdMarshalling[P <: Protocol] {
  type Cmd[_] <: Command[_]
  type In
  type Rslt

  val marshall: Marshall[Cmd, In, P#Request]
  val unmarshall: Unmarshall[P#Response, Rslt]
}

object CmdMarshalling {
  trait Aux[P <: Protocol, Cmd0[_] <: Command[_], In0, Rslt0] extends CmdMarshalling[P] {
    type Cmd[X] = Cmd0[X]
    type In = In0
    type Rslt = Rslt0
  }

  trait Anon[P <: Protocol, In, Rslt] extends Aux[P, AnonCmd, In, Rslt]
  trait Auth[P <: Protocol, In, Rslt] extends Aux[P, AuthCmd, In, Rslt]

  /*implicit def apply[P <: Protocol, Cmd0[_] <: Command[_], In0, Rslt0](
    implicit req: Marshall[Cmd0, In0, P#Request], res: Unmarshall[P#Response, Rslt0]): ProtocolOp.Aux[P, Cmd0, In0, Rslt0] =
    new Aux[P, Cmd0, In0, Rslt0] {
      override val marshall = req
      override val unmarshall = res
    }*/

  //implicit def apply[P <: Protocol, Cmd0[_] <: Command[_], In0, Rslt0](implicit ev)
  //implicit def from[P <: Protocol, In, Rslt](marshall: Marshall)
}

/*trait MarshallingResolver[P <: Protocol, Cmd[_] <: Command[_], In, Rslt] {
  def apply(): Marshalling.Aux[P, Cmd, In, Rslt]
}

object MarshallingResolver {
  implicit def apply[P <: Protocol, Cmd[_] <: Command[_], In, Rslt]
    (implicit ev: Marshalling.Aux[P, Cmd, In, Rslt]): MarshallingResolver[P, Cmd, In, Rslt] =
    new MarshallingResolver[P, Cmd, In, Rslt] {
      @inline override def apply(): Aux[P, Cmd, In, Rslt] = ev
    }
}*/
