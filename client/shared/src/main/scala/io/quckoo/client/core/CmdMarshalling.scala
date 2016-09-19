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
