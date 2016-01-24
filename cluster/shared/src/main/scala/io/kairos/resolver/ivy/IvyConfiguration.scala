package io.kairos.resolver.ivy

import java.io.File
import java.net.URL
import java.nio.file.Paths

import com.typesafe.config.Config
import io.kairos.resolver.{MavenRepository, Repository}

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
class IvyConfiguration private (val baseDir: File,
                                val cacheDir: File,
                                val ivyHome: Option[File],
                                val repositories: Seq[Repository] = Nil) {

}

object IvyConfiguration {
  final val BaseDir = "resolver.workDir"
  final val HomeDir = "resolver.home"
  final val CacheDir = "resolver.cacheDir"
  final val Repositories = "resolver.repositories"

  val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal
    //Repository.sbtLocal("local")
  )

  def apply(config: Config): IvyConfiguration = {
    val baseDir = createPathIfNotExists(config.getString(BaseDir))
    val cacheDir = createPathIfNotExists(config.getString(CacheDir))
    val repositories = config.getConfigList(Repositories).map { repoConf =>
      MavenRepository(repoConf.getString("name"), new URL(repoConf.getString("url")))
    }
    if (config.hasPath(HomeDir)) {
      new IvyConfiguration(baseDir, cacheDir,
        Some(createPathIfNotExists(config.getString(HomeDir))),
        repositories
      )
    } else {
      new IvyConfiguration(baseDir, cacheDir, None, repositories)
    }
  }

  private[this] def createPathIfNotExists(pathName: String): File = {
    val file = Paths.get(pathName).toAbsolutePath.toFile
    file.mkdirs()
    file
  }

}
