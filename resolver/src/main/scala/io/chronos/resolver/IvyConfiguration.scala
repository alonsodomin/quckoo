package io.chronos.resolver

import java.net.URL
import java.nio.file.{Path, Paths}

import com.typesafe.config.Config

import scala.collection.JavaConversions._

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
class IvyConfiguration private (val baseDir: Path,
                                val cacheDir: Path,
                                val ivyHome: Option[Path],
                                val repositories: Seq[Repository] = Nil) {

}

object IvyConfiguration {
  val BaseDir = "resolver.workDir"
  val HomeDir = "resolver.home"
  val CacheDir = "resolver.cacheDir"
  val Repositories = "resolver.repositories"

  val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal
    //Repository.sbtLocal("local")
  )

  def apply(config: Config): IvyConfiguration = {
    val baseDir = Paths.get(config.getString(BaseDir)).toAbsolutePath
    val cacheDir = Paths.get(config.getString(CacheDir)).toAbsolutePath
    val repositories = config.getConfigList(Repositories).map { repoConf =>
      MavenRepository(repoConf.getString("name"), new URL(repoConf.getString("url")))
    }
    if (config.hasPath(HomeDir)) {
      new IvyConfiguration(baseDir, cacheDir,
        Some(Paths.get(config.getString(HomeDir)).toAbsolutePath),
        repositories
      )
    } else {
      new IvyConfiguration(baseDir, cacheDir, None, repositories)
    }
  }

}
