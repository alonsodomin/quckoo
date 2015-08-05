package io.chronos.scheduler.internal.cluster

/**
 * Created by aalonsodominguez on 04/08/15.
 */
trait ClusterSync {

  def synchronize(f: => Unit): Unit

}
