package io.chronos

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object protocol {

  trait ClientEvent

  case object Connect
  case object Connected extends ClientEvent

  case object Disconnect
  case object Disconnected extends ClientEvent

  case object GetClusterStatus

  case class ResolutionFailed(unresolvedDependencies: Seq[String])

  type JobRejectedCause = Either[ResolutionFailed, Throwable]


}
