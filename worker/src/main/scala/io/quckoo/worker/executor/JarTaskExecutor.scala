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

package io.quckoo.worker.executor

import akka.actor.Props
import akka.pattern._

import cats.{Monad, ~>}
import cats.data.{Coproduct, EitherT}
import cats.effect.IO
import cats.free.Free

import io.quckoo._
import io.quckoo.reflect._
import io.quckoo.reflect.javareflect._
import io.quckoo.resolver._
import io.quckoo.worker.core.{TaskExecutor, WorkerContext}

/**
  * Created by alonsodomin on 16/02/2017.
  */
object JarTaskExecutor {

  type ArtifactOp[A] = Coproduct[ResolverOp, ReflectOp, A]
  type ArtifactIO[A] = Free[ArtifactOp, A]

  implicit def interpreter[F[_]: Monad](implicit resolver: ResolverInterpreter[F], reflector: ReflectorInterpreter[F]): ArtifactOp ~> F =
    resolver or reflector

  implicit class ArtifactIOOps[A](val self: ArtifactIO[A]) extends AnyVal {
    def to[F[_]: Monad](implicit interpreter: ArtifactOp ~> F): F[A] =
      self.foldMap(interpreter)
  }

  def props(workerContext: WorkerContext, taskId: TaskId, jarPackage: JarJobPackage): Props =
    Props(new JarTaskExecutor(workerContext, taskId, jarPackage))

}

class JarTaskExecutor private (
    workerContext: WorkerContext,
    taskId: TaskId,
    jarPackage: JarJobPackage)
  extends TaskExecutor {

  import TaskExecutor._
  import JarTaskExecutor._

  def receive: Receive = ready

  private[this] def ready: Receive = {
    case Run =>
      import context.dispatcher

      log.info("Starting execution of task '{}' using class '{}' from artifact {}.",
        taskId, jarPackage.jobClass, jarPackage.artifactId
      )

      downloadAndRun.shift.unsafeToFuture().pipeTo(sender())
  }

  private def downloadAndRun(implicit resolver: InjectableResolver[ArtifactOp], reflect: InjectableReflector[ArtifactOp]): IO[Response] = {
    import resolver._, reflect._

    def downloadJars: ArtifactIO[Either[Failed, Artifact]] =
      download(jarPackage.artifactId).map(_.leftMap(errors => Failed(MissingDependencies(errors))).toEither)

    def runIt(artifact: Artifact): ArtifactIO[Unit] = for {
      jobClass <- loadJobClass(artifact, jarPackage.jobClass)
      job      <- createJob(jobClass)
      _        <- runJob(job)
    } yield ()

    implicit val myResolver = workerContext.resolver

    EitherT(downloadJars)
      .semiflatMap(runIt)
      .value.to[IO]
      .attempt
      .map(_.fold(ex => Left(Failed(ExceptionThrown.from(ex))), identity))
      .map(_.fold(identity, _ => Completed(())))
  }

}
