/*
 * Copyright 2016 Antonio Alonso Dominguez
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

import diode.ActionType

import monix.execution.Ack
import monix.execution.Ack.Continue
import monix.reactive.Observer

import slogging.LazyLogging

import scala.concurrent.Future

/**
  * Created by alonsodomin on 02/04/2016.
  */
class SimpleEventSubscriber[A : ActionType] extends Observer[A] with LazyLogging {

  override def onError(ex: Throwable): Unit = {
    logger.error(s"Event stream throw an exception.", ex)
  }

  override def onComplete(): Unit = ()

  override def onNext(elem: A): Future[Ack] = {
    ConsoleCircuit.dispatch(elem)
    Future.successful(Continue)
  }

}
