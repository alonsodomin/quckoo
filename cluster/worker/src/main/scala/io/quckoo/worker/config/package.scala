package io.quckoo.worker

import akka.actor.{AddressFromURIString, RootActorPath}

import pureconfig._

/**
  * Created by alonsodomin on 04/11/2016.
  */
package object config {

  implicit val contactPointConfig: ConfigConvert[ContactPoint] = ConfigConvert.fromNonEmptyString {
    case AddressFromURIString(addr) => new ContactPoint(RootActorPath(addr) / "system" / "receptionist")
  }

}
