package io.quckoo.time

import org.threeten.bp.Clock

/**
  * Created by alonsodomin on 11/08/2016.
  */
object implicits {

  implicit val systemClock = Clock.systemUTC

}
