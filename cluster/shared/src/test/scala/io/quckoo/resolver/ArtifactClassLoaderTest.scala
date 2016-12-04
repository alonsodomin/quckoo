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

package io.quckoo.resolver

import java.net.URL

import org.scalatest.{FlatSpec, Inside, Matchers}

/**
 * Created by aalonsodominguez on 25/07/15.
 */
class ArtifactClassLoaderTest extends FlatSpec with Matchers with Inside {

  private val CommonsLoggingURL = new URL("http://repo1.maven.org/maven2/commons-logging/commons-logging-api/1.1/commons-logging-api-1.1.jar")

  "An ArtifactClassLoader" should "load any class from an URL" in {
    val classLoader = new ArtifactClassLoader(Array(CommonsLoggingURL))
    val loggerClass = classLoader.loadClass("org.apache.commons.logging.Log")
    loggerClass should not be null
  }

  it should "throw ClassNotFoundException when asked to load a non existent class" in {
    val classLoader = new ArtifactClassLoader(Array(CommonsLoggingURL))
    intercept[ClassNotFoundException] {
      classLoader.loadClass("com.example.FakeClass")
    }
  }

}
