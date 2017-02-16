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

package io.quckoo.console.registry

import io.quckoo.JobSpec
import io.quckoo.id.JobId

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 09/04/2016.
  */
object JobSelect {

  case class Props(jobs: Map[JobId, JobSpec],
                   value: Option[JobId],
                   onUpdate: Option[JobId] => Callback)

  private[this] val JobOption = ReactComponentB[(JobId, JobSpec)]("JobOption").stateless.render_P {
    case (jobId, spec) =>
      val desc = spec.description.map(text => s"| $text").getOrElse("")
      <.option(^.value := jobId.toString(), s"${spec.displayName} $desc")
  } build

  class Backend($ : BackendScope[Props, Unit]) {

    def onUpdate(evt: ReactEventI): Callback = {
      val newValue = {
        if (evt.target.value.isEmpty) None
        else Some(JobId(evt.target.value))
      }

      $.props.flatMap(_.onUpdate(newValue))
    }

    def render(props: Props) = {
      <.div(
        ^.`class` := "form-group",
        <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "jobId", "Job"),
        <.div(
          ^.`class` := "col-sm-10",
          <.select(
            ^.id := "jobId",
            ^.`class` := "form-control",
            props.value.map(id => ^.value := id.toString()),
            ^.onChange ==> onUpdate,
            <.option("Select a job"),
            props.jobs
              .filter(!_._2.disabled)
              .map(jobPair => JobOption.withKey(jobPair._1.toString)(jobPair)))))
    }

  }

  val component = ReactComponentB[Props]("JobSelect").stateless.renderBackend[Backend].build

  def apply(jobs: Map[JobId, JobSpec], value: Option[JobId], onUpdate: Option[JobId] => Callback) =
    component(Props(jobs, value, onUpdate))

}
