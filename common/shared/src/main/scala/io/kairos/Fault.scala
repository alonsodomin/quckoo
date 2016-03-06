package io.kairos

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

sealed trait ResolutionFailed extends Fault

case class UnresolvedDependency(artifactId: ArtifactId) extends ResolutionFailed {
  override def toString = artifactId.toString
}

case class DownloadFailed(artifactName: String) extends ResolutionFailed

// == Validation errors ====================

sealed trait ValidationFault extends Fault

case class NotNull(msg: String) extends ValidationFault
case class Required(msg: String) extends ValidationFault