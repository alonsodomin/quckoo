package io.chronos.examples

import java.util.UUID

import io.chronos.JobSpec

/**
 * Created by domingueza on 10/07/15.
 */
case object PowerOfNJobSpec extends PowerOfNJobSpec

class PowerOfNJobSpec() extends JobSpec(id = UUID.randomUUID(), displayName = "Power Of N", jobClass = classOf[PowerOfNJob])
