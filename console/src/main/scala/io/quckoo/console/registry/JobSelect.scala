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

import io.quckoo.{JobId, JobSpec}
import io.quckoo.console.layout.lookAndFeel

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 09/04/2016.
  */
object JobSelect {
  @inline private def lnf = lookAndFeel

  case class Props(jobs: Map[JobId, JobSpec],
                   value: Option[JobId],
                   onUpdate: Option[JobId] => Callback,
                   readOnly: Boolean = false)

  private[this] val JobOption = ScalaComponent.builder[(JobId, JobSpec)]("JobOption").stateless.render_P {
    case (jobId, spec) =>
      val desc = spec.description.map(text => s"| $text").getOrElse("")
      <.option(^.value := jobId.toString(), s"${spec.displayName} $desc")
  } build

  class Backend($ : BackendScope[Props, Unit]) {

    def onUpdate(evt: ReactEventFromInput): Callback = {
      val newValue = {
        if (evt.target.value.isEmpty) None
        else Some(JobId(evt.target.value))
      }

      $.props.flatMap(_.onUpdate(newValue))
    }

    def render(props: Props) = {
      <.div(lnf.formGroup,
        <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "jobId", "Job"),
        <.div(
          ^.`class` := "col-sm-10",
          <.select(lnf.formControl,
            ^.id := "jobId",
            props.value.map(id => ^.value := id.toString()).whenDefined,
            ^.onChange ==> onUpdate,
            ^.readOnly := props.readOnly,
            ^.disabled := props.readOnly,
            <.option("Select a job"),
            props.jobs
              .filter(!_._2.disabled)
              .toVdomArray(jobPair => JobOption.withKey(jobPair._1.toString)(jobPair))
          )
        )
      )
    }

  }

  val Component = ScalaComponent.builder[Props]("JobSelect")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(jobs: Map[JobId, JobSpec],
            value: Option[JobId],
            onUpdate: Option[JobId] => Callback,
            readOnly: Boolean = false) = Component(Props(jobs, value, onUpdate, readOnly))

}
