package io.quckoo.time

/**
  * Created by alonsodomin on 14/03/2016.
  */
class DummyDateTime(val value: Long) extends DateTime {
  type Repr = Long

  override def diff(that: DateTime): Duration =
    new DummyDuration(value - that.toEpochMillis)

  override def plusHours(hours: Int): DateTime =
    plusMinutes(hours * 60)

  override def plusMillis(millis: Long): DateTime =
    new DummyDateTime(value + millis)

  override def plusSeconds(seconds: Int): DateTime =
    plusMillis(seconds * 1000)

  override def plusMinutes(minutes: Int): DateTime =
    plusSeconds(minutes * 60)

  override def equals(other: Any): Boolean = other match {
    case that: DummyDateTime => this.value == that.value
    case _                   => false
  }

  override def hashCode: Int = value.hashCode()

  override def format(pattern: String): String =
    value.toString

  override def underlying = value

  override def toLocal: DateTime = this

  override def toUTC: DateTime = this

  override def toEpochSecond: Long = value / 1000

  override def toEpochMillis: Long = value

  override def toString = value.toString
}
