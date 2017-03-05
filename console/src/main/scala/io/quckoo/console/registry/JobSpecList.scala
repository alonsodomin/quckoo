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

import io.quckoo.{JobId, JobSpec}
import io.quckoo.console.components._
import io.quckoo.console.core.LoadJobSpecs
import io.quckoo.protocol.registry._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalaz._
import scalaz.syntax.show._

/**
  * Created by alonsodomin on 17/10/2015.
  */
object JobSpecList {

  final val Columns = List('Name, 'Description, 'Package, 'Status)

  final val DisabledFilter: Table.Filter[JobId, JobSpec] = (_, job) => job.disabled
  final val EnabledFilter: Table.Filter[JobId, JobSpec]  = !DisabledFilter(_, _)

  final case class Props(proxy: ModelProxy[PotMap[JobId, JobSpec]], onJobClick: JobSpec => Callback)
  final case class State(filter: Table.Filter[JobId, JobSpec])

  class Backend($ : BackendScope[Props, State]) {

    def mounted(props: Props) = {
      def dispatchJobLoading: Callback =
        props.proxy.dispatchCB(LoadJobSpecs)

      Callback.when(props.proxy().size == 0)(dispatchJobLoading)
    }

    private[this] def renderName(jobId: JobId, jobSpec: JobSpec): ReactNode =
      <.a(^.onClick --> jobClicked(jobId), jobSpec.displayName)

    def renderItem(jobId: JobId, jobSpec: JobSpec, column: Symbol): ReactNode = column match {
      case 'Name        => renderName(jobId, jobSpec)
      case 'Description => jobSpec.description.getOrElse[String]("")
      case 'Package     => jobSpec.jobPackage.toString
      case 'Status =>
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

    def jobClicked(jobId: JobId): Callback = {
      def onJobClickedCB(jobSpec: JobSpec): Callback =
        $.props.flatMap(_.onJobClick(jobSpec))

      def jobIsNotReady: Callback =
        Callback.alert(s"Job '$jobId' is not ready yet.")

      $.props.map(_.proxy()).flatMap(
        _.get(jobId).headOption.map(onJobClickedCB).getOrElse(jobIsNotReady)
      )
    }

    def filterClicked(filterType: Symbol): Callback = filterType match {
      case 'All      => $.modState(_.copy(filter = Table.NoFilter))
      case 'Enabled  => $.modState(_.copy(filter = EnabledFilter))
      case 'Disabled => $.modState(_.copy(filter = DisabledFilter))
    }

    def render(p: Props, state: State) = {
      val model = p.proxy()

      NavBar(
        NavBar
          .Props(List('All, 'Enabled, 'Disabled), 'All, filterClicked, style = NavStyle.pills),
        Table(
          Columns,
          model.seq,
          renderItem,
          allowSelect = true,
          actions = Some(rowActions(p)(_, _)),
          filter = Some(state.filter),
          style = Set(TableStyle.hover))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("JobSpecList")
    .initialState(State(Table.NoFilter))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.mounted($.props))
    .build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]], onJobClick: JobSpec => Callback) =
    component(Props(proxy, onJobClick))

}
