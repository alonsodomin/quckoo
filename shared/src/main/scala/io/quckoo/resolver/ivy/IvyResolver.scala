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

package io.quckoo.resolver.ivy

import java.net.URL

import cats.data.Validated
import cats.effect.IO
import cats.syntax.cartesian._
import cats.syntax.validated._
import cats.syntax.show._

import io.quckoo.reflect.Artifact
import io.quckoo.resolver.config.IvyConfig
import io.quckoo.resolver.{Resolved, Resolver}
import io.quckoo.{
  ArtifactId,
  DependencyError,
  DownloadFailed,
  UnresolvedDependency
}

import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{
  Configuration,
  DefaultDependencyDescriptor,
  DefaultModuleDescriptor,
  ModuleDescriptor
}
import org.apache.ivy.core.module.id.{ModuleRevisionId => IvyModuleId}
import org.apache.ivy.core.report.{ArtifactDownloadReport, ResolveReport}
import org.apache.ivy.core.resolve.ResolveOptions

import slogging.LazyLogging

/**
  * Created by alonsodomin on 03/05/2017.
  */
object IvyResolver {

  private final val DefaultConfName = "default"
  private final val CompileConfName = "compile"
  private final val RuntimeConfName = "runtime"

  private final val Configurations =
    Array(DefaultConfName, CompileConfName, RuntimeConfName)

  def apply(config: IvyConfig): IvyResolver = {
    val ivy = Ivy.newInstance(config)
    new IvyResolver(ivy)
  }

}

class IvyResolver private[ivy] (ivy: Ivy)
    extends Resolver[IO]
    with LazyLogging {
  import IvyResolver._

  override def validate(artifactId: ArtifactId): IO[Resolved[ArtifactId]] =
    resolve(artifactId, download = false).map(_.map(_ => artifactId))

  override def download(artifactId: ArtifactId): IO[Resolved[Artifact]] =
    resolve(artifactId, download = true)

  private[this] def resolve(artifactId: ArtifactId,
                            download: Boolean): IO[Resolved[Artifact]] = IO {
    def unresolvedDependencies(report: ResolveReport): Resolved[ResolveReport] = {
      val validations: List[Validated[DependencyError, ResolveReport]] =
        report.getUnresolvedDependencies
          .map(_.getId)
          .map { moduleId =>
            val unresolvedId =
              ArtifactId(moduleId.getOrganisation,
                         moduleId.getName,
                         moduleId.getRevision)
            UnresolvedDependency(unresolvedId).invalid[ResolveReport]
        } toList

      validations.foldLeft(report.validNel[DependencyError])((acc, v) =>
        (acc |@| v.toValidatedNel).map((_, r) => r))
    }

    def downloadFailed(report: ResolveReport): Resolved[ResolveReport] = {
      val validations: List[Validated[DependencyError, ResolveReport]] =
        report.getFailedArtifactsReports.map { artifactReport =>
          val moduleRevisionId = artifactReport.getArtifact.getModuleRevisionId
          val artifactId = ArtifactId(moduleRevisionId.getOrganisation,
                                      moduleRevisionId.getName,
                                      moduleRevisionId.getRevision)
          val reason = {
            if (artifactReport.getDownloadDetails == ArtifactDownloadReport.MISSING_ARTIFACT) {
              DownloadFailed.NotFound
            } else {
              DownloadFailed.Other(artifactReport.getDownloadDetails)
            }
          }
          DownloadFailed(artifactId, reason).invalid[ResolveReport]
        } toList

      validations.foldLeft(report.validNel[DependencyError])((acc, v) =>
        (acc |@| v.toValidatedNel).map((_, r) => r))
    }

    def artifactLocations(
        artifactReports: Seq[ArtifactDownloadReport]): Seq[URL] = {
      for (report <- artifactReports) yield {
        val localFile = Option(report.getUnpackedLocalFile)
          .orElse(Option(report.getLocalFile))

        localFile match {
          case Some(file) => Right(file)
          case None       => Left(report.getArtifact.getUrl)
        }
      } fold (identity, _.toURI.toURL)
    }

    val moduleDescriptor = newCallerInstance(artifactId)
    val resolveOptions = new ResolveOptions()
      .setTransitive(true)
      .setValidate(true)
      .setDownload(download)
      .setOutputReport(false)
      .setConfs(Array(DefaultConfName))

    logger.debug(s"Resolving $moduleDescriptor")
    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    (unresolvedDependencies(resolveReport) |@| downloadFailed(resolveReport))
      .map { (_, r) =>
        val artifactClasspath =
          artifactLocations(r.getAllArtifactsReports).toList

        logger.debug(
          "Job package created for artifact {} and classpath: {}",
          artifactId.show,
          artifactClasspath.mkString(":")
        )
        Artifact(artifactId, artifactClasspath)
      }
  }

  private[this] def newCallerInstance(
      artifactId: ArtifactId): ModuleDescriptor = {
    val moduleRevisionId: IvyModuleId = IvyModuleId.newInstance(
      artifactId.organization,
      artifactId.name,
      artifactId.version
    )

    val descriptor = new DefaultModuleDescriptor(
      IvyModuleId.newInstance(moduleRevisionId.getOrganisation,
                              moduleRevisionId.getName + "-job",
                              "working"),
      "integration",
      null,
      true)
    Configurations.foreach(c =>
      descriptor.addConfiguration(new Configuration(c)))
    descriptor.setLastModified(System.currentTimeMillis)

    val dependencyDescriptor =
      new DefaultDependencyDescriptor(descriptor,
                                      moduleRevisionId,
                                      false,
                                      false,
                                      true)
    Configurations.foreach(c =>
      dependencyDescriptor.addDependencyConfiguration(c, c))
    descriptor.addDependency(dependencyDescriptor)

    descriptor
  }

}
