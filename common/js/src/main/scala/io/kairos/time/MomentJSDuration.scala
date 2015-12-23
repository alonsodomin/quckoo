package io.kairos.time

import org.widok.moment.{Duration => MDuration}

/**
  * Created by alonsodomin on 23/12/2015.
  */
class MomentJSDuration(d: MDuration) extends Duration {

  def toMillis: Long = d.milliseconds()

}
