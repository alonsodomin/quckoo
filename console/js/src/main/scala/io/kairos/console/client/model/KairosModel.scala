package io.kairos.console.client.model

import io.kairos.JobSpec

/**
  * Created by alonsodomin on 20/02/2016.
  */
case class KairosModel private (jobSpecs: Seq[JobSpec])

object KairosModel {
  def initial = KairosModel(Seq.empty[JobSpec])
}
