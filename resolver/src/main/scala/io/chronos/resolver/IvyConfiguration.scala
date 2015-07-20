package io.chronos.resolver

import java.nio.file.{Path, Paths}

import com.typesafe.config.Config

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
class IvyConfiguration private (val baseDir: Path, val ivyHome: Option[Path], val repositories: Seq[Repository] = Nil) {

  def :+ (repository: Repository): IvyConfiguration =
    new IvyConfiguration(baseDir, ivyHome, repositories :+ repository)

  def ++ (repos: Seq[Repository]): IvyConfiguration =
    new IvyConfiguration(baseDir, ivyHome, repositories ++ repos)

}

object IvyConfiguration {
  val BaseDir = "ivy.workDir"
  val HomeDir = "ivy.home"

  val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal,
    Repository.sbtLocal("local")
  )

  def apply(config: Config): IvyConfiguration = {
    val baseDir = Paths.get(config.getString(BaseDir))
    if (config.hasPath(HomeDir)) {
      new IvyConfiguration(baseDir, Some(Paths.get(config.getString(HomeDir))))
    } else {
      new IvyConfiguration(baseDir, None)
    }
  }

}
