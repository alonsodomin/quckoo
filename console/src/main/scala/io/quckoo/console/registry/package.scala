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

package io.quckoo.console

import io.quckoo.console.core.ConsoleCircuit.Implicits.consoleClock
import io.quckoo.console.core.RegisterJobResult
import io.quckoo.console.log._
import io.quckoo.protocol.Event
import io.quckoo.protocol.registry.{JobDisabled, JobEnabled}

/**
  * Created by alonsodomin on 14/05/2017.
  */
package object registry {

  final val RegistryLogger: Logger[Event] = {
    case RegisterJobResult(validated) =>
      validated.fold(
        errs => LogRecord.error(""),
        jobId => LogRecord.info(s"Job '$jobId' has been registered."))

    case JobEnabled(jobId) =>
      LogRecord.info(s"Job '$jobId' has been enabled")

    case JobDisabled(jobId) =>
      LogRecord.info(s"Job '$jobId' has been disabled")
  }

}
