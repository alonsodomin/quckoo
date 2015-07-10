package io.chronos.example

import java.util.UUID

import io.chronos.JobSpec

/**
 * Created by domingueza on 10/07/15.
 */
case object PowerOfNJobSpec extends JobSpec(id = UUID.randomUUID().toString, displayName = "Power Of N", jobClass = classOf[PowerOfNJob])
