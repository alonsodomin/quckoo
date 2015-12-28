package io.kairos

import io.kairos.id.ArtifactId
import io.kairos.protocol.ResolutionFailed

import scala.language.implicitConversions

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver {

  type Resolve = (ArtifactId, Boolean) => Either[ResolutionFailed, Artifact]

}
