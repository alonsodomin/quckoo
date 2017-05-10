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

package io.quckoo.worker.executor

import java.io.IOException
import java.nio.file.attribute.PosixFilePermission

import better.files._

import org.scalatest._

class ProcessRunnerSpec extends AsyncFlatSpec with Matchers {

  "ProcessRunner" should "run shell scripts and capture its standard output" in {
    val expectedOut = "Hello World!"

    val runner = new ProcessRunner("echo", expectedOut)
    runner.run.map { result =>
      result.exitCode shouldBe 0
      result.stdOut shouldBe s"$expectedOut\n"
      result.stdErr shouldBe ""
    } unsafeToFuture()
  }

  it should "also capture the standad error" in {
    val expectedOut = "Hello Err!"

    val script = s"""
    | #!/bin/bash
    | >&2 echo $expectedOut
    """.stripMargin

    val scriptFile = File.newTemporaryFile()
    scriptFile.append(script)
    scriptFile.addPermission(PosixFilePermission.OWNER_EXECUTE)

    val runner = new ProcessRunner(scriptFile.path.toString)
    runner.run.map { result =>
      result.exitCode shouldBe 0
      result.stdOut shouldBe ""
      result.stdErr shouldBe s"$expectedOut\n"
    } unsafeToFuture()
  }

  it should "capture the exit code when explicitly set" in {
    val expectedExitCode = 123
    val script = s"""
    | #!/bin/bash
    | exit $expectedExitCode
    """.stripMargin

    val scriptFile = File.newTemporaryFile()
    scriptFile.append(script)
    scriptFile.addPermission(PosixFilePermission.OWNER_EXECUTE)

    val runner = new ProcessRunner(scriptFile.path.toString)
    runner.run.map { result =>
      result.exitCode shouldBe expectedExitCode
    } unsafeToFuture()
  }

  it should "return exit code 127 when the script uses a non-existent command" in {
    val runner = new ProcessRunner("bash", "foo")
    runner.run.map { result =>
      result.exitCode shouldBe 127
    } unsafeToFuture()
  }

  it should "fail with an IO Exception when attempting to run an invalid command" in {
    val runner = new ProcessRunner("foo")
    recoverToSucceededIf[IOException] {
      runner.run.unsafeToFuture()
    }
  }

}
