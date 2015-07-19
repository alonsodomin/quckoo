package io.chronos.resolver

import io.chronos.id.JobModuleId
import io.chronos.protocol._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.core.module.id.ModuleRevisionId
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

  private lazy val ivy = Ivy.newInstance(config)

  def resolve(jobModuleId: JobModuleId, download: Boolean): Either[JobModulePackage, ResolutionFailed] = {
    val resolveOptions = new ResolveOptions()
    resolveOptions.setTransitive(true)
    resolveOptions.setDownload(download)
    resolveOptions.setOutputReport(false)

    val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact + "-cache", jobModuleId.version)
    )

    val moduleId = ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact, jobModuleId.version)
    val dependencyDescriptor = new DefaultDependencyDescriptor(moduleDescriptor, moduleId, false, false, true)
    dependencyDescriptor.addDependencyConfiguration("default", "master")

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    if (resolveReport.hasError) {
      Right(ResolutionFailed(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
    } else {
      val artifactUrls = resolveReport.getAllArtifactsReports.map(_.getLocalFile.toURI.normalize.toURL)
      val artifactUrlsAsStr = artifactUrls.mkString(", ")
      log.info(s"Resolver artifact URLs: $artifactUrlsAsStr")
      Left(JobModulePackage(jobModuleId, artifactUrls))
    }
  }

}
