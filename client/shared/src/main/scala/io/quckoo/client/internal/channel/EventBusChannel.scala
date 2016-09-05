package io.quckoo.client.internal.channel

/**
  * Created by alonsodomin on 05/09/2016.
  */
trait EventBusChannel[In, Out] extends ListenerChannel[Out] {

  def send(input: In): Unit

}
