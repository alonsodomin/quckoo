/*
 * Copyright 2016 Antonio Alonso Dominguez
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

package io.quckoo.resolver.ivy

import java.io.File
import java.net.URL
import java.nio.file.Paths

import com.typesafe.config.Config
import io.quckoo.resolver.{MavenRepository, Repository}

import scala.collection.JavaConverters._

/**
  * Created by aalonsodominguez on 19/07/2015.
  */
class IvyConfiguration private (
    val baseDir: File,
    val resolutionDir: File,
    val repositoryDir: File,
    val ivyHome: Option[File],
    val repositories: Seq[Repository]
)

object IvyConfiguration {
  final val Namespace = "resolver"

  final val BaseDir       = "work-dir"
  final val HomeDir       = "home"
  final val ResolutionDir = "resolution-cache-dir"
  final val RepositoryDir = "repository-cache-dir"
  final val Repositories  = "repositories"

  val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal
    //Repository.sbtLocal("local")
  )

  def apply(config: Config): IvyConfiguration = {
    val baseDir       = createPathIfNotExists(config.getString(BaseDir))
    val resolutionDir = createPathIfNotExists(config.getString(ResolutionDir))
    val repositoryDir = createPathIfNotExists(config.getString(RepositoryDir))

    val repositories = config.getConfigList(Repositories).asScala.map { repoConf =>
      MavenRepository(repoConf.getString("name"), new URL(repoConf.getString("url")))
    }

    if (config.hasPath(HomeDir)) {
      new IvyConfiguration(
        baseDir,
        resolutionDir,
        repositoryDir,
        Some(createPathIfNotExists(config.getString(HomeDir))),
        repositories)
    } else {
      new IvyConfiguration(baseDir, resolutionDir, repositoryDir, None, repositories)
    }
  }

  private[this] def createPathIfNotExists(pathName: String): File = {
    val file = Paths.get(pathName).toAbsolutePath.toFile
    file.mkdirs()
    file
  }

}
