package io.kairos

import io.kairos.id.ModuleId
import io.kairos.protocol.ResolutionFailed

import scala.language.implicitConversions

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver {

  type Resolve = (ModuleId, Boolean) => Either[ResolutionFailed, JobPackage]

}
