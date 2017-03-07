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
import io.quckoo.console.core.ConsoleScope
import io.quckoo.protocol.registry._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.Defaults._
import scalacss.ScalaCssReact._

import scalaz.OptionT

/**
  * Created by alonsodomin on 17/10/2015.
  */
object RegistryPage {
  import ScalazReact._

  object Style extends StyleSheet.Inline {
    import dsl._

    val content = style(addClassName("container"))

  }

  final case class Props(proxy: ModelProxy[ConsoleScope])

  private lazy val formRef = Ref.to(JobForm.component, "jobForm")

  class Backend($ : BackendScope[Props, Unit]) {

    def jobForm: OptionT[CallbackTo, JobForm.Backend] =
      OptionT(CallbackTo.lift(() => formRef($).toOption.map(_.backend)))

    def editJob(spec: Option[JobSpec]): Callback = {
      jobForm.flatMapF(_.editJob(spec)).getOrElseF(Callback.empty)
    }

    def jobEdited(spec: Option[JobSpec]): Callback = {
      def dispatchAction(props: Props): Callback =
        spec.map(RegisterJob).map(props.proxy.dispatchCB[RegisterJob]).getOrElse(Callback.empty)

      $.props >>= dispatchAction
    }

    def render(props: Props) = {
      val connector = props.proxy.connect(_.userScope.jobSpecs)

      <.div(
        Style.content,
        <.h2("Registry"),
        JobForm(jobEdited, formRef.name),
        connector(JobSpecList(_,
          editJob(None),
          jobSpec => editJob(Some(jobSpec))
        ))
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("RegistryPage")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(proxy: ModelProxy[ConsoleScope]) = component(Props(proxy))

}
