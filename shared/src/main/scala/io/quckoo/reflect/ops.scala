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

package io.quckoo.reflect

import cats.free.Free

import io.quckoo.{Job, JobClass}

/**
  * Created by alonsodomin on 04/05/2017.
  */
object ops extends Reflector[ReflectIO] {

  override def loadJobClass(artifact: Artifact, className: String): ReflectIO[JobClass] =
    Free.liftF(ReflectOp.LoadJobClass(artifact, className))

  override def createJob(jobClass: JobClass): ReflectIO[Job] =
    Free.liftF(ReflectOp.CreateJob(jobClass))

  override def runJob(job: Job): ReflectIO[Unit] =
    Free.liftF(ReflectOp.RunJob(job))

}
