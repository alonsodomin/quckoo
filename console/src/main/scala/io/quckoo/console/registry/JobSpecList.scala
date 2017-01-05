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

import scalaz._
import scalaz.syntax.show._

/**
  * Created by alonsodomin on 17/10/2015.
  */
object JobSpecList {

  final val Columns = List("Name", "Description", "Artifact ID", "Job Class", "Status")

  final val AllFilter: Table.Filter[JobId, JobSpec]      = (id, job) => true
  final val DisabledFilter: Table.Filter[JobId, JobSpec] = (id, job) => job.disabled
  final val EnabledFilter: Table.Filter[JobId, JobSpec]  = !DisabledFilter(_, _)

  final case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]])
  final case class State(filter: Table.Filter[JobId, JobSpec])

  class Backend($ : BackendScope[Props, State]) {

    def mounted(props: Props) = {
      def dispatchJobLoading: Callback =
        props.proxy.dispatchCB(LoadJobSpecs)

      Callback.when(props.proxy().size == 0)(dispatchJobLoading)
    }

    def renderItem(jobId: JobId, jobSpec: JobSpec, column: String): ReactNode = column match {
      case "Name"        => jobSpec.displayName
      case "Description" => jobSpec.description.getOrElse[String]("")
      case "Artifact ID" => jobSpec.artifactId.shows
      case "Job Class"   => jobSpec.jobClass
      case "Status" =>
        if (jobSpec.disabled) {
          <.span(^.color.red, "DISABLED")
        } else {
          <.span(^.color.green, "ENABLED")
        }
    }

    def enableJob(props: Props)(jobId: JobId): Callback =
      props.proxy.dispatchCB(EnableJob(jobId))

    def disableJob(props: Props)(jobId: JobId): Callback =
      props.proxy.dispatchCB(DisableJob(jobId))

    def rowActions(props: Props)(jobId: JobId, jobSpec: JobSpec) = {
      Seq(if (jobSpec.disabled) {
        Table.RowAction[JobId, JobSpec](NonEmptyList(Icons.play, "Enable"), enableJob(props))
      } else {
        Table.RowAction[JobId, JobSpec](NonEmptyList(Icons.stop, "Disable"), disableJob(props))
      })
    }

    def filterClicked(filterType: String): Callback = filterType match {
      case "All"      => $.modState(_.copy(filter = AllFilter))
      case "Enabled"  => $.modState(_.copy(filter = EnabledFilter))
      case "Disabled" => $.modState(_.copy(filter = DisabledFilter))
    }

    def render(p: Props, state: State) = {
      val model = p.proxy()

      NavBar(
        NavBar
          .Props(List("All", "Enabled", "Disabled"), "All", filterClicked, style = NavStyle.pills),
        Table(
          Columns,
          model.seq,
          renderItem,
          allowSelect = true,
          actions = Some(rowActions(p)(_, _)),
          filter = Some(state.filter))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("JobSpecList")
    .initialState(State(AllFilter))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.mounted($.props))
    .build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]]) = component(Props(proxy))

}
