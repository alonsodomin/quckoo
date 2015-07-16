package io.chronos.examples.parameters

import java.util.UUID

import io.chronos.JobSpec

/**
 * Created by domingueza on 10/07/15.
 */
case object PowerOfNJobSpec extends PowerOfNJobSpec

class PowerOfNJobSpec() extends JobSpec(
  id = UUID.randomUUID(),
  displayName = "Power Of N",
  moduleId = "io.chronos:chronos-examples:0.1.0",
  jobClass = classOf[PowerOfNJob]
)
