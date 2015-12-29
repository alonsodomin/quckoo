package io.kairos

import io.kairos.id.ArtifactId
import io.kairos.protocol.Error

import scala.language.implicitConversions
import scalaz.ValidationNel

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver {

  type ResolutionResult = ValidationNel[Error, Artifact]
  type Resolve = (ArtifactId, Boolean) => ResolutionResult

}
