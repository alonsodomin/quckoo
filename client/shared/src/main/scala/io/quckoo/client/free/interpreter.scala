package io.quckoo.client.free

import io.quckoo.client.core.Protocol
import io.quckoo.client.free.driver.Driver
import io.quckoo.client.free.marshalling.Marshaller
import io.quckoo.util._

import scalaz._
import Scalaz._
import scala.concurrent.Future

/**
  * Created by domingueza on 03/11/2016.
  */
object interpreter {

  trait Interpreter[Op[_]] {
    type FreeOp[A] = Free[Op, A]

    def interp[P <: Protocol](implicit marshaller: Marshaller[P]): Op ~> Kleisli[Future, Driver[P], ?] = new (Op ~> Kleisli[Future, Driver[P], ?]) {
      def apply[A](op: Op[A]): Kleisli[Future, Driver[P], A] = Kleisli { driver =>
        val marshall = marshaller.marshall[Op[A]].transform(attempt2Future)
        val unmarshall = marshaller.unmarshall[A].transform(attempt2Future)

        (marshall >=> driver.send >=> unmarshall).run(op)
      }
    }

  }

}
