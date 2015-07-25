package io.chronos.resolver

import java.net.URL

import io.chronos.id.JobModuleId
import io.chronos.protocol._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor._
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.resolve.ResolveOptions
import org.slf4s.Logging

/**
 * Created by aalonsodominguez on 17/07/15.
 */
trait JobModuleResolver {

  def resolve(jobModuleId: JobModuleId, download: Boolean = false): Either[JobModulePackage, ResolutionFailed]

}

class IvyJobModuleResolver(config: IvyConfiguration) extends JobModuleResolver with Logging {
  private implicit val logger = log

  private val DefaultConfName = "default"
  private val CompileConfName = "compile"
  private val RuntimeConfName = "runtime"

  private lazy val ivy = Ivy.newInstance(config)

  def resolve(jobModuleId: JobModuleId, download: Boolean): Either[JobModulePackage, ResolutionFailed] = {
    val configurations = Array(DefaultConfName, CompileConfName, RuntimeConfName)

    val moduleDescriptor = DefaultModuleDescriptor.newCallerInstance(jobModuleId, configurations, true, true)

    val resolveOptions = new ResolveOptions().
      setTransitive(true).
      setValidate(true).
      setDownload(true).
      setOutputReport(false).
      setConfs(Array(RuntimeConfName))

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    if (resolveReport.hasError) {
      Right(ResolutionFailed(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
    } else {
      Left(JobModulePackage(jobModuleId, artifactLocations(resolveReport.getAllArtifactsReports)))
    }
  }

  private def artifactLocations(artifactReports: Seq[ArtifactDownloadReport]): Seq[URL] = {
    for (report <- artifactReports) yield {
      Option(report.getUnpackedLocalFile).
        orElse(Option(report.getLocalFile)) match {
        case Some(file) => Right(file)
        case None       => Left(report.getArtifact.getUrl)
      }
    } fold (identity, _.toURI.toURL)
  }

  private implicit def toModuleRevisionId(jobModuleId: JobModuleId): ModuleRevisionId = {
    ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact, jobModuleId.version)
  }

}
