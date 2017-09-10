/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.console.core

import diode.{ActionProcessor, ActionResult, Dispatcher}

import cats.syntax.order._

import io.quckoo.console.model.ConsoleScope
import io.quckoo.console.log._
import io.quckoo.protocol.Event

import monix.reactive.Observable
import monix.reactive.subjects.PublishSubject

/**
  * Created by alonsodomin on 07/05/2017.
  */
class EventLogProcessor(level: LogLevel, logger: Logger[Event])
    extends ActionProcessor[ConsoleScope] {

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
