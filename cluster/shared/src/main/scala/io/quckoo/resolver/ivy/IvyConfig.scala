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

  implicit val createFileOnLoad: ConfigConvert[File] =
    ConfigConvert.stringConvert(createPathIfNotExists, _.getAbsolutePath)

  private[this] def createPathIfNotExists(pathName: String): File = {
    val file = Paths.get(pathName).toAbsolutePath.toFile
    file.mkdirs()
    file
  }

}