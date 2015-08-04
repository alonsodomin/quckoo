package io.chronos.scheduler.store

import java.util.{Comparator, Map => JMap}

import com.hazelcast.core.IMap
import com.hazelcast.query.{PagingPredicate, Predicate}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 04/08/15.
 */
object HazelcastTraversable {

  def apply[K, V](source: IMap[K, V], ordering: Ordering[(K, V)], pageSize: Int): Traversable[(K, V)] =
    apply(source, ordering, pageSize, (_, _) => true)

  def apply[K, V](source: IMap[K, V], ordering: Ordering[(K, V)], pageSize: Int, f: (K, V) => Boolean): Traversable[(K, V)] = {
    val pageCount = source.size() / pageSize

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

    val traversable = new Traversable[(K, V)] {
      override def foreach[U](f: ((K, V)) => U): Unit = do {
        val values: Set[JMap.Entry[K, V]] = source.entrySet(paging).toSet
        for (entry <- values) f(entry.getKey, entry.getValue)
        paging.nextPage()
      } while (paging.getPage < pageCount)
    }
    traversable.view
  }

}
