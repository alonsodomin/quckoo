package io.kairos.console.client.time

import monocle.macros.Lenses

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 31/01/2016.
  */
@Lenses
case class AmountOfTime(amount: Int = 0, unit: TimeUnit = SECONDS)
