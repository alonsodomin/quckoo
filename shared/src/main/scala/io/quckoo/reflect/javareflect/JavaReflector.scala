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

package io.quckoo.reflect.javareflect

import cats.effect.IO
import cats.syntax.show._

import io.quckoo.reflect.{Artifact, Reflector}
import io.quckoo.{Job, JobClass}

import slogging.LazyLogging

/**
  * Created by alonsodomin on 04/05/2017.
  */
class JavaReflector private[reflect] () extends Reflector[IO] with LazyLogging {

  override def loadJobClass(artifact: Artifact,
                            className: String): IO[JobClass] = IO {
    logger.debug("Loading job class {} from artifact {}",
                 className,
                 artifact.artifactId.show)
    artifact.classLoader.loadClass(className).asInstanceOf[JobClass]
  }

  override def createJob(jobClass: JobClass): IO[Job] = IO {
    jobClass.newInstance().asInstanceOf[Job]
  }

  override def runJob(job: Job): IO[Unit] = IO { job.call(); () }

}
