package io.quckoo.console

import cats.Order

/**
  * Created by alonsodomin on 07/05/2017.
  */
package object log {
  type Logger[A] = PartialFunction[A, LogRecord]

  implicit val logLevelOrdering: Ordering[LogLevel] = Ordering.by(_.value)

  implicit val logLevelOrder: Order[LogLevel] = Order.fromOrdering

}
