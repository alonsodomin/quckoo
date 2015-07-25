package io.chronos.resolver

import java.nio.file.{Path, Paths}

import com.typesafe.config.Config

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
class IvyConfiguration private (val baseDir: Path,
                                val cacheDir: Path,
                                val ivyHome: Option[Path],
                                val repositories: Seq[Repository] = Nil) {

}

object IvyConfiguration {
  val BaseDir = "ivy.workDir"
  val HomeDir = "ivy.home"
  val CacheDir = "ivy.cacheDir"

  val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal,
    Repository.sbtLocal("local")
  )

  def apply(config: Config): IvyConfiguration = {
    val baseDir = Paths.get(config.getString(BaseDir)).toAbsolutePath
    val cacheDir = Paths.get(config.getString(CacheDir)).toAbsolutePath
    if (config.hasPath(HomeDir)) {
      new IvyConfiguration(baseDir, cacheDir, Some(Paths.get(config.getString(HomeDir)).toAbsolutePath))
    } else {
      new IvyConfiguration(baseDir, cacheDir, None)
    }
  }

}
