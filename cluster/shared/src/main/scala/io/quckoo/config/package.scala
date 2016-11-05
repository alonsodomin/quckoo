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

package io.quckoo

import java.io.File
import java.nio.file.Paths

import pureconfig.{CamelCase, ConfigConvert, ConfigFieldMapping, KebabCase}

/**
  * Created by alonsodomin on 04/11/2016.
  */
package object config {

  implicit def clusterFieldMapping[A]: ConfigFieldMapping[A] =
    ConfigFieldMapping.apply[A](CamelCase, KebabCase)

  implicit val createFileOnLoad: ConfigConvert[File] =
    ConfigConvert.stringConvert(path => Paths.get(path).toAbsolutePath.toFile, _.getAbsolutePath)

}
