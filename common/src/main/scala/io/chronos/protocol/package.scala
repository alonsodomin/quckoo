package io.chronos

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object protocol {

  case object Connect
  case object Connected

  case object Disconnect
  case object Disconnected

  case class ResolutionFailed(unresolvedDependencies: Seq[String])

  type JobRejectedCause = Either[ResolutionFailed, Throwable]


}
