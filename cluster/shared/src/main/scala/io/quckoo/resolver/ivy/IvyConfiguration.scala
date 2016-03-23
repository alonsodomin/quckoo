package io.quckoo.resolver.ivy

import java.io.File
import java.net.URL
import java.nio.file.Paths

import com.typesafe.config.Config
import io.quckoo.resolver.{MavenRepository, Repository}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
class IvyConfiguration private (
   val baseDir: File,
   val resolutionDir: File,
   val repositoryDir: File,
   val ivyHome: Option[File],
   val repositories: Seq[Repository] = Seq.empty)

object IvyConfiguration {
  final val BaseDir = "resolver.work-dir"
  final val HomeDir = "resolver.home"
  final val ResolutionDir = "resolver.resolution-cache-dir"
  final val RepositoryDir = "resolver.repository-cache-dir"
  final val Repositories = "resolver.repositories"

  val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal
    //Repository.sbtLocal("local")
  )

  def apply(config: Config): IvyConfiguration = {
    val baseDir = createPathIfNotExists(config.getString(BaseDir))
    val resolutionDir = createPathIfNotExists(config.getString(ResolutionDir))
    val repositoryDir = createPathIfNotExists(config.getString(RepositoryDir))

    val repositories = config.getConfigList(Repositories).map { repoConf =>
      MavenRepository(repoConf.getString("name"), new URL(repoConf.getString("url")))
    }

    if (config.hasPath(HomeDir)) {
      new IvyConfiguration(baseDir, resolutionDir, repositoryDir,
        Some(createPathIfNotExists(config.getString(HomeDir))),
        repositories
      )
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
