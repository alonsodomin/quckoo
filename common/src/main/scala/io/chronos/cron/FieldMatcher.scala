package io.chronos.cron

/**
 * Created by aalonsodominguez on 02/08/15.
 */
sealed trait FieldMatcher {
  
}

object FieldMatcher {

  case object Any extends FieldMatcher {
    override def toString = "*"
  }
  case class Exact(value: Int) extends FieldMatcher {
    override def toString = value.toString
  }
  case class Every(frequency: Int) extends FieldMatcher {
    override def toString = "*/" + frequency
  }
  
}
