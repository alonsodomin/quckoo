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

package io.quckoo.console.registry

import diode.react.ModelProxy

import io.quckoo._
import io.quckoo.console.model.ConsoleScope
import io.quckoo.console.layout.CssSettings._
import io.quckoo.protocol.registry._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 17/10/2015.
  */
object RegistryPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))

  }

  final case class Props(proxy: ModelProxy[ConsoleScope])

  class Backend($ : BackendScope[Props, Unit]) {

    private val jobFormRef = ScalaComponent.mutableRefTo(JobForm.component)

    def editJob(spec: Option[JobSpec]): Callback =
      CallbackTo(jobFormRef).flatMap(_.value.backend.editJob(spec))

    def jobEdited(spec: Option[JobSpec]): Callback = {
      def dispatchAction(props: Props): Callback =
        spec
          .map(RegisterJob)
          .map(props.proxy.dispatchCB[RegisterJob])
          .getOrElse(Callback.empty)

      $.props >>= dispatchAction
    }

    def render(props: Props) = {
      val connector = props.proxy.connect(_.userScope.jobSpecs)

      <.div(
        Style.content,
        <.h2("Registry"),
        jobFormRef.component(JobForm.Props(jobEdited)),
        connector(JobSpecList(_, editJob(None), jobSpec => editJob(Some(jobSpec))))
      )
    }

  }

  private[this] val component = ScalaComponent
    .builder[Props]("RegistryPage")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
