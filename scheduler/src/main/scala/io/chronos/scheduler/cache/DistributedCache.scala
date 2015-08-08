package io.chronos.scheduler.cache

/**
 * Created by aalonsodominguez on 08/08/15.
 */
trait DistributedCache[K, V] extends (K => V) {

  def get(k: K): Option[V]

  def toTraversable: Traversable[(K, V)]

}
