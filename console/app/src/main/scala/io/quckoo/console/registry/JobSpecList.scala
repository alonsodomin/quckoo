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

import diode.AnyAction._
import diode.data.PotMap
import diode.react.ModelProxy

import io.quckoo.JobSpec
import io.quckoo.console.components._
import io.quckoo.console.core.LoadJobSpecs
import io.quckoo.id.JobId
import io.quckoo.protocol.registry._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalaz.NonEmptyList

/**
 * Created by alonsodomin on 17/10/2015.
 */
object JobSpecList {

  final val Columns = List("Name", "Description", "Artifact ID", "Job Class", "Status")

  case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]])

  class Backend($: BackendScope[Props, Unit]) {

    def mounted(props: Props) = {
      def dispatchJobLoading: Callback =
        props.proxy.dispatch(LoadJobSpecs)

      Callback.when(props.proxy().size == 0)(dispatchJobLoading)
    }

    def renderItem(jobId: JobId, jobSpec: JobSpec, column: String): ReactNode = column match {
      case "Name" => jobSpec.displayName
      case "Description" => jobSpec.description.getOrElse[String]("")
      case "Artifact ID" => jobSpec.artifactId.toString
      case "Job Class"   => jobSpec.jobClass
      case "Status"      =>
        if (jobSpec.disabled) {
          <.span(^.color.red, "DISABLED")
        } else {
          <.span(^.color.green, "ENABLED")
        }
    }

    def enableJob(props: Props)(jobId: JobId): Callback =
      props.proxy.dispatch(EnableJob(jobId))

    def disableJob(props: Props)(jobId: JobId): Callback =
      props.proxy.dispatch(DisableJob(jobId))

    def rowActions(props: Props)(jobId: JobId, jobSpec: JobSpec) = {
      Seq(if (jobSpec.disabled) {
        Table.RowAction[JobId, JobSpec](NonEmptyList(Icons.play, "Enable"), enableJob(props))
      } else {
        Table.RowAction[JobId, JobSpec](NonEmptyList(Icons.stop, "Disable"), disableJob(props))
      })
    }

    def render(p: Props) = {
      val model = p.proxy()

      Table(Columns, model.seq, renderItem,
        allowSelect = true,
        actions = Some(rowActions(p)(_, _))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("JobSpecList").
    stateless.
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]]) = component(Props(proxy))

}
