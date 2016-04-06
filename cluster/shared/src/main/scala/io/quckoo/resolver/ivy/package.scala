package io.quckoo.resolver

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.ChainResolver
import org.slf4s.Logging

/**
  * Created by alonsodomin on 28/12/2015.
  */
package object ivy extends Logging {
  import scala.language.implicitConversions

  private[ivy] implicit def convertConfig2Settings(config: IvyConfiguration): IvySettings = {
    implicit val ivySettings = new IvySettings()
    ivySettings.setBaseDir(config.baseDir)
    ivySettings.setDefaultResolutionCacheBasedir(config.resolutionDir.getAbsolutePath)
    ivySettings.setDefaultRepositoryCacheBasedir(config.repositoryDir.getAbsolutePath)

    log.debug(s"Using default cache dir: ${ivySettings.getDefaultResolutionCacheBasedir}")
    log.debug(s"Using default repository dir: ${ivySettings.getDefaultRepositoryCacheBasedir}")

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

    ivySettings
  }

  private[this] def buildResolverChain(name: String, repos: Seq[Repository])(implicit settings: IvySettings): ChainResolver = {
    val resolver = new ChainResolver
    resolver.setName(name)
    repos.foreach { repo => resolver.add(RepositoryConversion(repo, settings)) }
    resolver
  }

}
