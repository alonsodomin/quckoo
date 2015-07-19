package io.chronos

import org.apache.ivy.core.settings.IvySettings
import org.slf4s.Logger

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
package object resolver {

  private[resolver] implicit def convertConfig2Settings(config: IvyConfiguration)(implicit log: Logger): IvySettings = {
    val ivySettings = new IvySettings()
    ivySettings.setBaseDir(config.baseDir.toFile)
    ivySettings.setDefaultIvyUserDir(config.ivyHome.toFile)
    val defaultRepo = config.repositories.head
    config.repositories.foreach { repo =>
      ivySettings.addResolver(RepositoryConversion(repo, ivySettings))
    }
    ivySettings.setDefaultResolver(defaultRepo.name)
    log.info("Apache Ivy initialized")
    ivySettings
  }

}
