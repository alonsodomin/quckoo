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

package io.quckoo.resolver.config

import java.io.File

import io.quckoo.resolver.{MavenRepository, Repository}

/**
  * Created by domingueza on 03/11/2016.
  */
final case class IvyConfig(
    baseDir: File,
    resolutionCacheDir: File,
    repositoryCacheDir: File,
    ivyHome: Option[File] = None,
    repositories: List[MavenRepository] = List.empty
) {

  def createFolders(): Unit = {
    val folders = List(baseDir, resolutionCacheDir, repositoryCacheDir) ++ ivyHome
    folders.foreach(_.mkdirs())
  }

}

object IvyConfig {

  final val DefaultRepositories = Seq(
    Repository.mavenCentral,
    Repository.mavenLocal
    //Repository.sbtLocal("local")
  )

}