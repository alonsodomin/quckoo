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

import cats.data.NonEmptyList
import cats.instances.list._
import cats.syntax.traverse._
import cats.syntax.show._
import diode.AnyAction._
import diode.data.{Pot, PotMap, Ready}
import diode.react.ModelProxy
import io.quckoo.{JobId, JobSpec}
import io.quckoo.console.components._
import io.quckoo.console.core.LoadJobSpecs
import io.quckoo.console.layout.ContextStyle
import io.quckoo.protocol.registry._
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Created by alonsodomin on 17/10/2015.
  */
object JobSpecList {
  import CatsReact._

  final val Columns = List('Name, 'Description, 'Package, 'Status)

  final val DisabledFilter: Table.Filter[JobId, JobSpec] = (_, job) => job.disabled
  final val EnabledFilter: Table.Filter[JobId, JobSpec]  = !DisabledFilter(_, _)

  final val Filters: Map[Symbol, Table.Filter[JobId, JobSpec]] = Map(
    'Enabled  -> EnabledFilter,
    'Disabled -> DisabledFilter
  )

  type OnCreate = Callback
  type OnClick = JobSpec => Callback

  final case class Props(
    proxy: ModelProxy[PotMap[JobId, JobSpec]],
    onJobCreate: OnCreate,
    onJobEdit: OnClick
  )
  final case class State(selectedFilter: Option[Symbol] = None, selectedJobs: Set[JobId] = Set.empty)

  class Backend($ : BackendScope[Props, State]) {

    private[JobSpecList] def mounted(props: Props) = {
      def dispatchJobLoading: Callback =
        props.proxy.dispatchCB(LoadJobSpecs)

      Callback.when(props.proxy().size == 0)(dispatchJobLoading)
    }

    def selectedJobs(props: Props, state: State): Map[JobId, Pot[JobSpec]] = {
      val jobMap = props.proxy()
      jobMap.get(state.selectedJobs)
    }

    // Actions

    private[this] def performOnSelection(filter: (JobId, JobSpec) => Boolean)
                                        (f: JobId => RegistryCommand): Callback = {
      def collectIds: CallbackTo[Iterable[JobId]] = for {
        props <- $.props
        state <- $.state
      } yield selectedJobs(props, state).collect {
        case (id, Ready(spec)) if filter(id, spec) => id
      }

      def invokeCommand(jobIds: Iterable[JobId]): Callback = for {
        proxy <- $.props.map(_.proxy)
        _     <- jobIds.toList.map(id => proxy.dispatchCB(f(id))).sequence
      } yield ()

      collectIds >>= invokeCommand
    }

    def enableAll: Callback =
      performOnSelection((_, spec) => spec.disabled)(EnableJob)
    def enableAllDisabled(props: Props, state: State): Boolean = {
      val disabledJobs = selectedJobs(props, state).collect {
        case (_, Ready(spec)) if spec.disabled => spec
      }
      disabledJobs.isEmpty
    }

    def disableAll: Callback =
      performOnSelection((_, spec) => !spec.disabled)(DisableJob)
    def disableAllDisabled(props: Props, state: State): Boolean = {
      val enabledJobs = selectedJobs(props, state).collect {
        case (_, Ready(spec)) if !spec.disabled => spec
      }
      enabledJobs.isEmpty
    }

    def enableJob(props: Props)(jobId: JobId): Callback =
      props.proxy.dispatchCB(EnableJob(jobId))

    def disableJob(props: Props)(jobId: JobId): Callback =
      props.proxy.dispatchCB(DisableJob(jobId))

    def rowActions(props: Props)(jobId: JobId, jobSpec: JobSpec) = {
      Seq(if (jobSpec.disabled) {
        Table.RowAction[JobId, JobSpec](NonEmptyList.of(Icons.play, "Enable"), enableJob(props))
      } else {
        Table.RowAction[JobId, JobSpec](NonEmptyList.of(Icons.stop, "Disable"), disableJob(props))
      })
    }

    // Event handlers

    def onFilterClicked(filterType: Symbol): Callback =
      $.modState(_.copy(selectedFilter = Some(filterType)))

    def onJobClicked(jobId: JobId): Callback = {
      def onJobClickedCB(jobSpec: JobSpec): Callback =
        $.props.flatMap(_.onJobEdit(jobSpec))

      def jobIsNotReady: Callback =
        Callback.alert(s"Job '$jobId' is not ready yet.")

      $.props.map(_.proxy()).flatMap(
        _.get(jobId).headOption.map(onJobClickedCB).getOrElse(jobIsNotReady)
      )
    }

    def onJobSelected(selection: Set[JobId]): Callback =
      $.modState(_.copy(selectedJobs = selection))

    // Render methods

    private[this] def renderName(jobId: JobId, jobSpec: JobSpec): VdomNode =
      <.a(^.onClick --> onJobClicked(jobId), jobSpec.displayName)

    def renderItem(jobId: JobId, jobSpec: JobSpec, column: Symbol): VdomNode = column match {
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

    def render(props: Props, state: State) = {
      val model = props.proxy()

      <.div(
        ToolBar(
          Button(Button.Props(
            Some(props.onJobCreate),
            style = ContextStyle.primary
          ), Icons.plusSquare, "New Job"),
          Button(Button.Props(
            Some(enableAll),
            disabled = enableAllDisabled(props, state)
          ), Icons.playCircle, "Enable All"),
          Button(Button.Props(
            Some(disableAll),
            disabled = disableAllDisabled(props, state)
          ), Icons.stopCircle, "Disable All")
        ),
        NavBar(
          NavBar
            .Props(List('All, 'Enabled, 'Disabled), 'All, onFilterClicked, style = NavStyle.pills),
          Table(
            Columns,
            model.seq,
            renderItem,
            onSelect = Some(onJobSelected _),
            actions = Some(rowActions(props)(_, _)),
            filter = state.selectedFilter.flatMap(Filters.get),
            selected = state.selectedJobs,
            style = Set(TableStyle.hover))
        )
      )
    }

  }

  private[this] val component = ScalaComponent.builder[Props]("JobSpecList")
    .initialState(State())
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.mounted($.props))
    .build

  def apply(proxy: ModelProxy[PotMap[JobId, JobSpec]],
            onJobCreate: OnCreate,
            onJobClick: OnClick) =
    component(Props(proxy, onJobCreate, onJobClick))

}
