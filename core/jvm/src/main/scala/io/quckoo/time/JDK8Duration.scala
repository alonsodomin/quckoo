package io.quckoo.time

import java.time.{Duration => JDuration}

/**
  * Created by alonsodomin on 23/12/2015.
  */
class JDK8Duration(d: JDuration) extends Duration {

  def toMillis: Long = d.toMillis

}
