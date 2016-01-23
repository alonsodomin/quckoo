package io.kairos

import io.kairos.protocol.Fault

import scala.language.implicitConversions
import scalaz.ValidationNel

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver {

  type ResolutionResult = ValidationNel[Fault, Artifact]

}
