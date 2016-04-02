package io.quckoo.time

/**
  * Created by alonsodomin on 22/12/2015.
  */
trait TimeSource {

  def currentDateTime: DateTime

}
