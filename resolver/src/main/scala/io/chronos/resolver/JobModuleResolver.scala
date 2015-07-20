package io.chronos.resolver

import io.chronos.id.JobModuleId
import io.chronos.protocol._
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{Artifact, DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.{IvyNode, ResolveOptions}
import org.slf4s.Logging

import scala.collection.JavaConversions._

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
    val resolveOptions = new ResolveOptions().
      setTransitive(true).
      setDownload(download).
      setOutputReport(true).
      setConfs(Array("default"))

    val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact + "-job", jobModuleId.version)
    )
    moduleDescriptor.setDefaultConf("default")

    var jobModuleAttrs: Map[String, String] = Map.empty
    jobModuleId.scalaVersion.foreach { scalaVersion => jobModuleAttrs += ("scalaVersion" -> scalaVersion) }
    val moduleId = ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact, jobModuleId.version, jobModuleAttrs)

    val dependencyDescriptor = new DefaultDependencyDescriptor(moduleDescriptor, moduleId, false, false, true)
    dependencyDescriptor.addDependencyConfiguration("default", "master")
    moduleDescriptor.addDependency(dependencyDescriptor)

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)

    if (resolveReport.hasError) {
      Right(ResolutionFailed(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
    } else {
      val nodes = resolveReport.getDependencies.map ( n => n.asInstanceOf[IvyNode] )
      val artifacts = resolveReport.getArtifacts.map( a => a.asInstanceOf[Artifact] )
      //val artifactUrls = resolveReport.getAllArtifactsReports.map(_.getArtifact.getUrl)
      val artifactUrls = artifacts.map(_.getUrl)
      val artifactUrlsAsStr = artifactUrls.mkString(", ")
      log.info(s"Resolved artifact URLs: $artifactUrlsAsStr")
      Left(JobModulePackage(jobModuleId, artifactUrls))
    }
  }

}
