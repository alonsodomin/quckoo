package io.kairos

import java.util.UUID

import io.kairos.protocol.ErrorResponse

import scalaz.NonEmptyList

/**
 * Created by aalonsodominguez on 17/08/15.
 */
package object cluster {

  type WorkerId = UUID
  type TaskFailureCause = NonEmptyList[ErrorResponse]

}
