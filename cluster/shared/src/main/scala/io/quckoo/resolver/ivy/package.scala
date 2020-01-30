/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.resolver

import io.quckoo.resolver.config.IvyConfig

import org.apache.ivy.core.settings.IvySettings
import org.apache.ivy.plugins.resolver.ChainResolver

import slogging._

/**
  * Created by alonsodomin on 28/12/2015.
  */
package object ivy extends StrictLogging {
  import scala.language.implicitConversions

  private[ivy] implicit def convertConfig2Settings(config: IvyConfig): IvySettings = {
    implicit val ivySettings = new IvySettings()
    ivySettings.setBaseDir(config.baseDir)
    ivySettings.setDefaultResolutionCacheBasedir(config.resolutionCacheDir.getAbsolutePath)
    ivySettings.setDefaultRepositoryCacheBasedir(config.repositoryCacheDir.getAbsolutePath)

    logger.debug(s"Using default cache dir: ${ivySettings.getDefaultResolutionCacheBasedir}")
    logger.debug(s"Using default repository dir: ${ivySettings.getDefaultRepositoryCacheBasedir}")

    config.ivyHome match {
      case Some(home) => ivySettings.setDefaultIvyUserDir(home)
      case None       =>
    }

    val mainResolverChain = buildResolverChain(
      name = "main",
      IvyConfiguration.DefaultRepositories ++ config.repositories
    )
    ivySettings.addResolver(mainResolverChain)
    ivySettings.setDefaultResolver(mainResolverChain.getName)

    ivySettings
  }

  private[this] def buildResolverChain(name: String, repos: Seq[Repository])(
      implicit settings: IvySettings
  ): ChainResolver = {
    val resolver = new ChainResolver
    resolver.setName(name)
    repos.foreach { repo =>
      resolver.add(RepositoryConversion(repo, settings))
    }
    resolver
  }

}
