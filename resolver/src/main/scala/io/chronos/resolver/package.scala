package io.chronos

import io.chronos.id.ModuleId
import io.chronos.protocol.ResolutionFailed
import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.ChainResolver
import org.slf4s.Logging

import scala.language.implicitConversions

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver extends Logging {

  type ResolveFun = (ModuleId, Boolean) => Either[ResolutionFailed, JobPackage]
  
  private[resolver] implicit def convertConfig2Settings(config: IvyConfiguration): IvySettings = {
    implicit val ivySettings = new IvySettings()
    //ivySettings.loadDefault()
    ivySettings.setBaseDir(config.baseDir)
    ivySettings.setDefaultCache(config.cacheDir)
    config.ivyHome match {
      case Some(home) => ivySettings.setDefaultIvyUserDir(home)
      case None       =>
    }

    val mainResolverChain = buildResolverChain(
      name = "main",
      config.repositories ++ IvyConfiguration.DefaultRepositories
    )
    ivySettings.addResolver(mainResolverChain)
    ivySettings.setDefaultResolver(mainResolverChain.getName)

    log.info("Apache Ivy initialized")
    ivySettings
  }

  private def buildResolverChain(name: String, repos: Seq[Repository])(implicit settings: IvySettings): ChainResolver = {
    val resolver = new ChainResolver
    resolver.setName(name)
    repos.foreach { repo => resolver.add(RepositoryConversion(repo, settings)) }
    resolver
  }

}
