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

package io.quckoo.worker.executor

import akka.actor.{ActorRef, ActorRefFactory, Props}

import io.quckoo.{JarJobPackage, ShellScriptPackage, Task}
import io.quckoo.worker.config.ExecutorDispatcher
import io.quckoo.worker.core.{TaskExecutorProvider, WorkerContext}

/**
  * Created by alonsodomin on 16/02/2017.
  */
object DefaultTaskExecutorProvider extends TaskExecutorProvider {

  override def executorFor(context: WorkerContext, task: Task)(
      implicit actorRefFactory: ActorRefFactory
  ): ActorRef =
    task.jobPackage match {
      case jar: JarJobPackage =>
        val executorProps = configure(JarTaskExecutor.props(context, task.id, jar))
        actorRefFactory.actorOf(executorProps, "jar-executor")

      case shell: ShellScriptPackage =>
        val executorProps = configure(ShellTaskExecutor.props(context, task.id, shell))
        actorRefFactory.actorOf(executorProps, "shell-executor")
    }

  private[this] def configure(props: Props): Props =
    props.withDispatcher(ExecutorDispatcher)

}
