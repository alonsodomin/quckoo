package io.quckoo.time

import java.time.LocalTime
import java.time.temporal.ChronoField

/**
  * Created by alonsodomin on 09/04/2016.
  */
class JDK8Time(localTime: LocalTime) extends Time {
  override def hour: Int = localTime.getHour

  override def milliseconds: Int = localTime.get(ChronoField.MILLI_OF_SECOND)

  override def seconds: Int = localTime.getSecond

  override def minute: Int = localTime.getMinute

}
