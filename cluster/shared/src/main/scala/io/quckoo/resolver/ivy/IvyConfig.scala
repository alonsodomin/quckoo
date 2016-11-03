package io.quckoo.resolver.ivy

import java.io.File
import java.nio.file.Paths

import io.quckoo.resolver.{MavenRepository, Repository}

import pureconfig._

/**
  * Created by domingueza on 03/11/2016.
  */
case class IvyConfig(
  baseDir: File,
  resolutionDir: File,
  repositoryDir: File,
  ivyHome: Option[File],
  repositories: List[MavenRepository]
)

object IvyConfig {

  final val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal
    //Repository.sbtLocal("local")
  )

  implicit val createFileOnLoad = ConfigConvert.fromNonEmptyString(createPathIfNotExists)

  private[this] def createPathIfNotExists(pathName: String): File = {
    val file = Paths.get(pathName).toAbsolutePath.toFile
    file.mkdirs()
    file
  }
}