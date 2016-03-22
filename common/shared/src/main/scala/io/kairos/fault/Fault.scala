package io.kairos.fault

import io.kairos.id.ArtifactId

/**
  * Created by alonsodomin on 28/12/2015.
  */
sealed trait Fault extends Serializable

// == Generic errors ================

case class ExceptionThrown(className: String, message: String) extends Fault {

  override def toString: String = s"$className: $message"

}
object ExceptionThrown {
  def apply(t: Throwable): ExceptionThrown = ExceptionThrown(t.getClass.getName, t.getMessage)
}

// == Artifact resolution errors ============

sealed trait ResolutionFault extends Fault

case class UnresolvedDependency(artifactId: ArtifactId) extends ResolutionFault
case class DownloadFailed(artifactName: String) extends ResolutionFault

// == Validation errors ====================

sealed trait ValidationFault extends Fault

case class NotNull(msg: String) extends ValidationFault
case class Required(msg: String) extends ValidationFault
