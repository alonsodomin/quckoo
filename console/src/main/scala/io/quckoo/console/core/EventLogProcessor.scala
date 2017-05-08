package io.quckoo.console.core

import diode.{ActionProcessor, ActionResult, Dispatcher}

import cats.syntax.order._

import io.quckoo.console.log._
import io.quckoo.protocol.Event

import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

/**
  * Created by alonsodomin on 07/05/2017.
  */
class EventLogProcessor(level: LogLevel, logger: Logger[Event]) extends ActionProcessor[ConsoleScope] {

  private val emitter = PublishSubject[LogRecord]

  def logStream: Observable[LogRecord] = emitter

  override def process(
      dispatch: Dispatcher,
      action: Any,
      next: Any => ActionResult[ConsoleScope],
      currentModel: ConsoleScope
  ): ActionResult[ConsoleScope] = {
    action match {
      case event: Event if logger.isDefinedAt(event) =>
        val record = logger(event)
        if (record.level >= level) emitter.onNext(record)

      case _ =>
    }

    next(action)
  }
}
