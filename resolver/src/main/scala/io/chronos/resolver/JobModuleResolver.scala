package io.chronos.resolver

import java.net.URL

import io.chronos.id.JobModuleId
import io.chronos.protocol._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor._
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.ArtifactDownloadReport
import org.apache.ivy.core.resolve.ResolveOptions
import org.codehaus.plexus.classworlds.ClassWorld
import org.slf4s.Logging

/**
 * Created by aalonsodominguez on 17/07/15.
 */
trait JobModuleResolver {

  def resolve(jobModuleId: JobModuleId, download: Boolean = false)(implicit classWorld: ClassWorld): Either[JobModulePackage, ResolutionFailed]

}

class IvyJobModuleResolver(config: IvyConfiguration) extends JobModuleResolver with Logging {
  private implicit val logger = log

  private val DefaultConfName = "default"

  private lazy val ivy = Ivy.newInstance(config)

  def resolve(jobModuleId: JobModuleId, download: Boolean)(implicit classWorld: ClassWorld): Either[JobModulePackage, ResolutionFailed] = {
    val configurations = Array(DefaultConfName, "compile", "runtime")

    val moduleDescriptor = DefaultModuleDescriptor.newCallerInstance(jobModuleId, configurations, true, false)

    val resolveOptions = new ResolveOptions().
      setTransitive(true).
      setValidate(true).
      setDownload(true).
      setOutputReport(false).
      setResolveId(ResolveOptions.getDefaultResolveId(moduleDescriptor)).
      setConfs(configurations)

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    if (resolveReport.hasError) {
      Right(ResolutionFailed(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
    } else {
      Left(JobModulePackage(jobModuleId, artifactLocations(resolveReport.getAllArtifactsReports)))
    }
  }

  private def describeIvyModule(jobModuleId: JobModuleId): ModuleDescriptor = {
    DefaultModuleDescriptor.newCallerInstance(jobModuleId, Array(DefaultConfName), true, false)
  }

  private def defineIvyModule(jobModuleId: JobModuleId): ModuleDescriptor = {
    val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact + "-job", "working")
    )
    moduleDescriptor.setDefaultConf(DefaultConfName)

    val dependencyDescriptor = new DefaultDependencyDescriptor(moduleDescriptor, jobModuleId, false, false, true)
    dependencyDescriptor.addDependencyConfiguration(DefaultConfName, "*")
    moduleDescriptor.addDependency(dependencyDescriptor)

    moduleDescriptor
  }

  private def artifactLocations(artifactReports: Seq[ArtifactDownloadReport]): Seq[URL] = {
    for (report <- artifactReports) yield {
      Option(report.getUnpackedLocalFile).orElse(Option(report.getLocalFile)) match {
        case Some(file) => Right(file)
        case None       => Left(report.getArtifact.getUrl)
      }
    } fold (identity, _.toURI.toURL)
  }

  private implicit def toModuleRevisionId(jobModuleId: JobModuleId): ModuleRevisionId = {
    ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact, jobModuleId.version)
  }

}
