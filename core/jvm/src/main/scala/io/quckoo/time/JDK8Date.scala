package io.quckoo.time

import java.time.LocalDate

/**
  * Created by alonsodomin on 09/04/2016.
  */
class JDK8Date(localDate: LocalDate) extends Date {

  override def year: Int = localDate.getYear

  override def dayOfMonth: Int = localDate.getDayOfMonth

  override def month: Int = localDate.getMonthValue

}
