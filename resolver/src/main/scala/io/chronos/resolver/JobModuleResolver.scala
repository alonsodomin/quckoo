package io.chronos.resolver

import java.nio.file.Path

import io.chronos.id.JobModuleId
import org.apache.ivy.Ivy
import org.apache.ivy.core.module.id.{ModuleId, ModuleRevisionId}
import org.apache.ivy.core.resolve.ResolveOptions
import org.apache.ivy.core.settings.IvySettings

/**
 * Created by aalonsodominguez on 17/07/15.
 */
class JobModuleResolver(localRepo: Path) {

  private val ivySettings = new IvySettings()
  ivySettings.setBaseDir(localRepo.toFile)

  private val ivy = Ivy.newInstance(ivySettings)

  def resolve(jobModuleId: JobModuleId): Either[JobModulePackage, InvalidJobModule] = {
    val moduleId = new ModuleRevisionId(new ModuleId(jobModuleId.group, jobModuleId.artifact), jobModuleId.version)

    val resolveOptions = new ResolveOptions()
    resolveOptions.setDownload(false)

    val resolveReport = ivy.resolve(moduleId, resolveOptions, false)
    if (resolveReport.hasError) {
      Right(InvalidJobModule(resolveReport.getUnresolvedDependencies))
    } else {
      Left(JobModulePackage())
    }
  }

}
