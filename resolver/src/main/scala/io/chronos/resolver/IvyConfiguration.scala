package io.chronos.resolver

import java.nio.file.{Path, Paths}

import com.typesafe.config.Config

/**
 * Created by aalonsodominguez on 19/07/2015.
 */
class IvyConfiguration private (val baseDir: Path, val ivyHome: Path, val repositories: Seq[Repository] = Nil) {

  def :+ (repository: Repository): IvyConfiguration =
    new IvyConfiguration(baseDir, ivyHome, repositories :+ repository)

  def ++ (repos: Seq[Repository]): IvyConfiguration =
    new IvyConfiguration(baseDir, ivyHome, repositories ++ repos)

}

object IvyConfiguration {
  val BaseDir = "ivy.workDir"

  val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal,
    Repository.sbtLocal("local")
  )

  def apply(config: Config): IvyConfiguration = {
    val ivyHome = Paths.get(System.getProperty("user.home"), ".ivy2")
    apply(config, ivyHome)
  }

  def apply(config: Config, ivyHome: Path): IvyConfiguration = {
    val baseDir = config.getString(BaseDir)
    new IvyConfiguration(Paths.get(baseDir), ivyHome)
  }

}
