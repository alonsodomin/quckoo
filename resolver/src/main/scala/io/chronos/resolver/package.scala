package io.chronos

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.ChainResolver
import org.slf4s.Logger

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver {

  private[resolver] implicit def convertConfig2Settings(config: IvyConfiguration)(implicit log: Logger): IvySettings = {
    implicit val ivySettings = new IvySettings()
    ivySettings.setBaseDir(config.baseDir.toFile)
    ivySettings.setDefaultIvyUserDir(config.ivyHome.toFile)

    val defaultResolverChain = buildResolverChain("default", IvyConfiguration.DefaultRepositories)
    val userResolverChain = buildResolverChain("user", config.repositories)

    ivySettings.addResolver(defaultResolverChain)
    ivySettings.addResolver(userResolverChain)
    ivySettings.setDefaultResolver(defaultResolverChain.getName)

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
