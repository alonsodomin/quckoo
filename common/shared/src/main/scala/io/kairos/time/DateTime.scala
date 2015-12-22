package io.kairos.time

/**
  * Created by alonsodomin on 22/12/2015.
  */
abstract class DateTime {

  def plusMillis(millis: Long): DateTime

  def plusHours(hours: Int): DateTime

}
