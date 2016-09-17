package io.quckoo

import io.quckoo.client.core.{Driver, Protocol, Transport}

/**
  * Created by alonsodomin on 17/09/2016.
  */
package object client extends Protocols {

  implicit def driver[P <: Protocol](implicit transport: Transport.For[P]): Driver[P] =
    new Driver[P](transport)

}
