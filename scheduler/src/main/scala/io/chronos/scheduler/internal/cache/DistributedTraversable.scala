package io.chronos.scheduler.internal.cache

import java.util.{Collection => JCollection, Comparator, Map => JMap}

import com.hazelcast.core.{IMap => DMap}
import com.hazelcast.query.{PagingPredicate, Predicate}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object DistributedTraversable {

  def apply[K, V](source: DMap[K, V], ordering: Ordering[(K, V)], pageSize: Int): Traversable[(K, V)] =
    apply(source, ordering, pageSize, (_: K, _: V) => true)

  def apply[K, V](source: DMap[K, V], ordering: Ordering[(K, V)], pageSize: Int, f: (K, V) => Boolean): Traversable[(K, V)] = {
    val filter = new Predicate[K, V] {
      override def apply(entry: JMap.Entry[K, V]): Boolean =
        f(entry.getKey, entry.getValue)
    }
    val comparator = new Comparator[JMap.Entry[K, V]] {
      override def compare(o1: JMap.Entry[K, V], o2: JMap.Entry[K, V]): Int =
        ordering.compare((o1.getKey, o1.getValue), (o2.getKey, o2.getValue))
    }
    val paging = new PagingPredicate(
      filter.asInstanceOf[Predicate[_, _]],
      comparator.asInstanceOf[Comparator[JMap.Entry[_, _]]],
      pageSize
    )

    apply(source, paging)
  }

  def apply[K, V](source: DMap[K, V], paging: PagingPredicate): Traversable[(K, V)] = {
    val pageCount = source.size() / paging.getPageSize
    val traversable = new Traversable[(K, V)] {
      override def foreach[U](f: ((K, V)) => U): Unit = do {
        val values: JCollection[JMap.Entry[K, V]] = source.entrySet(paging)
        for (entry <- values) f(entry.getKey, entry.getValue)
        paging.nextPage()
      } while (paging.getPage < pageCount)
    }
    traversable.view
  }

}
