package io.chronos.resolver

import java.nio.file.Path

import io.chronos.id.JobModuleId
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.descriptor.{DefaultDependencyDescriptor, DefaultModuleDescriptor}
import org.apache.ivy.core.module.id.ModuleRevisionId
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.{DependencyResolver, URLResolver}

/**
 * Created by aalonsodominguez on 17/07/15.
 */
class IvyJobModuleResolver(workDir: Path) {

  private val ivySettings = new IvySettings()
  ivySettings.setBaseDir(workDir.toFile)

  val repositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal,
    Repository.sbtLocal("local")
  )
  repositories foreach { repo => ivySettings.addResolver(toResolver(repo)) }

  private def toResolver[T <: Repository](t: T): DependencyResolver = {
    val resolver = new URLResolver()
    t.patterns.artifactPatterns.foreach { resolver.addArtifactPattern }
    t.patterns.ivyPatterns.foreach { resolver.addIvyPattern }
    resolver
  }

  private val ivy = Ivy.newInstance(ivySettings)

  def resolve(jobModuleId: JobModuleId): Either[JobModulePackage, InvalidJobModule] = {
    val resolveOptions = new ResolveOptions()
    resolveOptions.setTransitive(true)
    resolveOptions.setDownload(false)
    resolveOptions.setOutputReport(false)

    val moduleDescriptor = DefaultModuleDescriptor.newDefaultInstance(
      ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact + "-cache", jobModuleId.version)
    )

    val moduleId = ModuleRevisionId.newInstance(jobModuleId.group, jobModuleId.artifact, jobModuleId.version)
    val dependencyDescriptor = new DefaultDependencyDescriptor(moduleDescriptor, moduleId, false, false, false)
    dependencyDescriptor.addDependencyConfiguration("default", "master")

    val resolveReport = ivy.resolve(moduleDescriptor, resolveOptions)
    if (resolveReport.hasError) {
      Right(InvalidJobModule(resolveReport.getUnresolvedDependencies.map(_.getModuleId.toString)))
    } else {
      Left(JobModulePackage())
    }
  }

}
