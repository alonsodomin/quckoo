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

package io.quckoo.reflect.javareflect

import java.net.URL

import cats.effect.IO

import io.quckoo.ArtifactId
import io.quckoo.reflect.Artifact
import io.quckoo.reflect.ops._

import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alonsodomin on 06/05/2017.
  */
class JavaReflectorSpec extends FlatSpec with Matchers {

  "JavaReflector" should "fail to a class that does not exist" in {
    val fooArtifactId = ArtifactId("com.example", "foo", "latest")
    val fooArtifact =
      Artifact(fooArtifactId, List(new URL("http://www.example.com")))

    val className = "com.example.Foo"

    val program = for {
      jobClass <- loadJobClass(fooArtifact, className)
      job <- createJob(jobClass)
      _ <- runJob(job)
    } yield ()

    val exception = intercept[ClassNotFoundException] {
      program.to[IO].unsafeRunSync()
    }

    exception.getMessage shouldBe className
  }

}
