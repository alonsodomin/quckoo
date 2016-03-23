package io.quckoo.time

/**
  * Created by alonsodomin on 14/03/2016.
  */
class DummyDuration(value: Long) extends Duration {
  override def toMillis: Long = value
}
