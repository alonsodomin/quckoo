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

import java.net.URL
import java.util.concurrent.Callable

import cats.{Eq, Show}
import cats.implicits._

import io.quckoo.{ArtifactId, JobClass}

import slogging._

import scala.util.{Failure, Success, Try}

/**
  * Created by aalonsodominguez on 17/07/15.
  */
object Artifact {

  implicit val artifactEq: Eq[Artifact] = Eq.by(_.artifactId)

  implicit val artifactShow: Show[Artifact] = Show.show { artifact =>

    show"${artifact.artifactId} ::  "
  }

}

final case class Artifact(
    artifactId: ArtifactId,
    classpath: List[URL]
  ) extends LazyLogging {

  logCreation()

  lazy val classLoader: ClassLoader = new ArtifactClassLoader(classpath.toArray)

  private[reflect] def loadClass(className: String): Try[Class[_]] =
    Try(classLoader.loadClass(className))

  def jobClass(className: String): Try[JobClass] = {
    logger.debug("Loading job class: {}", className)
    loadClass(className) map { _.asInstanceOf[JobClass] }
  }

  def newJob(className: String, params: Map[String, Any]): Try[Callable[_]] = {
    def injectParameters(clazz: JobClass, instance: Any): Try[Unit] = {
      val injection = Either.catchNonFatal(clazz.getDeclaredFields.toList).flatMap { list =>
        list.filter(f => params.contains(f.getName)).map { field =>
          Either.catchNonFatal {
            val value = params(field.getName)
            logger.debug("Injecting value '{}' into job instance of class '{}'", value, className)
            field.set(instance, value)
          }
        } sequenceU
      }

      injection match {
        case Left(ex) => Failure(ex)
        case Right(_) => Success(())
      }
    }

    for {
      clazz    <- jobClass(className)
      instance <- Try(clazz.newInstance())
      _        <- injectParameters(clazz, instance)
    } yield instance
  }

  override def equals(other: Any): Boolean = other match {
    case that: Artifact => artifactId == that.artifactId
    case _              => false
  }

  override def hashCode(): Int = artifactId.hashCode()

  private def logCreation(): Unit = {
    val classpathStr = classpath.mkString(":")
    logger.debug(s"Job package created for artifact ${artifactId.show} and classpath: $classpathStr")
  }

}
