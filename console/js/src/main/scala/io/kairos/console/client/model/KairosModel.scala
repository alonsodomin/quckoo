package io.kairos.console.client.model

import diode.data._
import io.kairos.JobSpec

/**
  * Created by alonsodomin on 20/02/2016.
  */
case class KairosModel private (jobSpecs: Pot[JobSpec])

object KairosModel {
  def initial = KairosModel(Empty)
}
