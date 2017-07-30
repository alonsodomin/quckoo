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

package io.quckoo.worker.core

import akka.actor.{ActorRef, ActorRefFactory}

import io.quckoo.Task

/**
  * Created by alonsodomin on 16/02/2017.
  */
trait TaskExecutorProvider {

  def executorFor(context: WorkerContext, task: Task)(
      implicit actorRefFactory: ActorRefFactory): ActorRef

}
