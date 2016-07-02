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

import diode.react.ModelProxy

import io.quckoo._
import io.quckoo.console.components._
import io.quckoo.console.core.ConsoleScope
import io.quckoo.protocol.registry._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 17/10/2015.
 */
object RegistryPageView {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))

  }

  case class Props(proxy: ModelProxy[ConsoleScope])
  case class State(
      selectedJob: Option[JobSpec] = None,
      showForm: Boolean = false
  )

  class RegistryBackend($: BackendScope[Props, State]) {

    def editJob(spec: Option[JobSpec]) =
      $.modState(_.copy(selectedJob = spec, showForm = true))

    def jobEdited(spec: Option[JobSpec]): Callback = {
      def dispatchAction(props: Props): Callback =
        spec.map(RegisterJob).map(props.proxy.dispatch[RegisterJob]).getOrElse(Callback.empty)

      def updateState(): Callback =
        $.modState(_.copy(showForm = false))

      updateState() >> ($.props >>= dispatchAction)
    }

    def render(props: Props, state: State) = {
      val connector = props.proxy.connect(_.userScope.jobSpecs)

      <.div(Style.content,
        <.h2("Registry"),
        props.proxy().notification,
        Button(Button.Props(Some(editJob(None))), Icons.plusSquare, "New Job"),
        if (state.showForm) JobForm(state.selectedJob, jobEdited)
        else EmptyTag,
        connector(JobSpecList(_))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("RegistryPage").
    initialState(State()).
    renderBackend[RegistryBackend].
    build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
