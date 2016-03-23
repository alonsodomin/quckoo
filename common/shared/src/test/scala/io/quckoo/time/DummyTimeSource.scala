package io.quckoo.time

/**
  * Created by alonsodomin on 14/03/2016.
  */
class DummyTimeSource(dateTime: DateTime) extends TimeSource {

  override def currentDateTime: DateTime = dateTime

}
