package io.chronos.scheduler.concurrent

/**
 * Created by aalonsodominguez on 04/08/15.
 */
trait ClusterSync {

  def synchronize(f: => Unit): Unit

}
