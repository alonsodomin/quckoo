package io.chronos.scheduler.concurrent

import com.hazelcast.core.HazelcastInstance

/**
 * Created by aalonsodominguez on 05/08/15.
 */
class NonBlockingSync(hazelcastInstance: HazelcastInstance, name: String) extends ClusterSync {

  private val mutex = hazelcastInstance.getAtomicReference[Boolean](name + "_mutex")
  mutex.set(false)

  override def synchronize(f: => Unit): Unit = if (mutex.compareAndSet(false, true)) {
    try f finally mutex.set(false)
  }

}
