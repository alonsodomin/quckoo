package io.chronos.resolver

import io.chronos.id.JobModuleId
import io.chronos.protocol._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor._
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.report.DownloadStatus
import org.apache.ivy.core.resolve.ResolveOptions
import org.slf4s.Logging

import scala.collection.JavaConversions._
import scala.collection.mutable

/**
 * Created by aalonsodominguez on 17/07/15.
 */
trait JobModuleResolver {

  def resolve(jobModuleId: JobModuleId, download: Boolean = false): Either[JobModulePackage, ResolutionFailed]

}

class IvyJobModuleResolver(config: IvyConfiguration) extends JobModuleResolver with Logging {
  private implicit val logger = log

  private val DefaultConfName = "default"

  private lazy val ivy = Ivy.newInstance(config)

  def resolve(jobModuleId: JobModuleId, download: Boolean): Either[JobModulePackage, ResolutionFailed] = {
    val resolveOptions = new ResolveOptions().
      setTransitive(true).
      setValidate(true).
      setDownload(download).
      setOutputReport(false).
      setConfs(Array(DefaultConfName))

    val moduleDescriptor = defineIvyModule(jobModuleId)
    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    if (resolveReport.hasError) {
      Right(ResolutionFailed(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
    } else {
      val artifactUrls = resolveReport.getAllArtifactsReports.view.
        filterNot(_.getDownloadStatus == DownloadStatus.FAILED).
        map(_.getArtifact.getUrl)
      Left(JobModulePackage(jobModuleId, artifactUrls))
    }
  }

  private def defineIvyModule(jobModuleId: JobModuleId): ModuleDescriptor = {
    val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact + "-job", "working")
    )
    moduleDescriptor.setDefaultConf(DefaultConfName)

    val jobModuleAttrs: mutable.Map[String, String] = mutable.Map.empty
    jobModuleId.scalaVersion.foreach { scalaVersion =>
      jobModuleAttrs.put("scalaVersion", scalaVersion)
    }
    val moduleId = ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact, jobModuleId.version, jobModuleAttrs)

    val dependencyDescriptor = new DefaultDependencyDescriptor(moduleDescriptor, moduleId, false, false, true)
    dependencyDescriptor.addDependencyConfiguration(DefaultConfName, "*")
    moduleDescriptor.addDependency(dependencyDescriptor)

    moduleDescriptor
  }

}
