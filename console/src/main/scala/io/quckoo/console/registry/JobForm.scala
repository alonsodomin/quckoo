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

import io.quckoo.{JobPackage, JobSpec}
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import monocle.macros.Lenses

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 23/12/2015.
  */
object JobForm {
  import MonocleReact._

  @inline
  private def lnf = lookAndFeel

  type Handler = Option[JobSpec] => Callback

  final case class Props(handler: Handler)

  @Lenses final case class EditableJobSpec(
    displayName: Option[String] = None,
    description: Option[String] = None,
    jobPackage: Option[JobPackage] = None
  ) {

    def this(jobSpec: Option[JobSpec]) =
      this(jobSpec.map(_.displayName), jobSpec.flatMap(_.description), jobSpec.map(_.jobPackage))

    def valid: Boolean =
      displayName.nonEmpty && jobPackage.nonEmpty

  }

  @Lenses final case class State(
      spec: EditableJobSpec,
      visible: Boolean = false,
      readOnly: Boolean = false,
      cancelled: Boolean = true
  )

  class Backend($: BackendScope[Props, State]) {

    val displayName = State.spec ^|-> EditableJobSpec.displayName
    val description = State.spec ^|-> EditableJobSpec.description
    val jobPackage  = State.spec ^|-> EditableJobSpec.jobPackage

    // Event handlers

    def onModalClosed(props: Props) = {
      def jobSpec(state: State): Option[JobSpec] = if (!state.cancelled) {
        for {
          name  <- state.spec.displayName
          pckg  <- state.spec.jobPackage
        } yield JobSpec(name, state.spec.description, pckg)
      } else None

      $.state.map(jobSpec) >>= props.handler
    }

    // Actions

    def submitForm(): Callback =
      $.modState(_.copy(visible = false, cancelled = false))

    def editJob(jobSpec: Option[JobSpec]): Callback =
      $.modState(_.copy(spec = new EditableJobSpec(jobSpec), visible = true, readOnly = jobSpec.isDefined))

    // Rendering

    val displayNameInput = Input[String]()
    val descriptionInput = Input[String]()

    def render(props: Props, state: State) = {
      <.form(^.name := "jobDetails", ^.`class` := "form-horizontal",
        if (state.visible) {
          Modal(
            Modal.Props(
              header = hide => <.span(
                <.button(^.tpe := "button", lookAndFeel.close, ^.onClick --> hide, Icons.close),
                <.h4("Register Job")
              ),
              footer = hide => <.span(
                Button(Button.Props(
                  Some(hide),
                  style = ContextStyle.default
                ), "Cancel"),
                Button(Button.Props(
                  Some(submitForm() >> hide),
                  style = ContextStyle.primary,
                  disabled = state.readOnly || !state.spec.valid
                ), "Save")
              ),
              onClosed = onModalClosed(props)
            ),
            <.div(lnf.formGroup,
              <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "displayName", "Display Name"),
              <.div(^.`class` := "col-sm-10",
                displayNameInput(
                  state.spec.displayName,
                  $.setStateL(displayName)(_),
                  ^.id := "displayName",
                  ^.placeholder := "Job's name",
                  ^.readOnly := state.readOnly
                )
              )
            ),
            <.div(lnf.formGroup,
              <.label(^.`class` := "col-sm-2 control-label", ^.`for` := "description", "Description"),
              <.div(^.`class` := "col-sm-10",
                descriptionInput(
                  state.spec.description,
                  $.setStateL(description)(_),
                  ^.id := "description",
                  ^.placeholder := "Job's description",
                  ^.readOnly := state.readOnly
                )
              )
            ),
            JobPackageSelect(state.spec.jobPackage, $.setStateL(jobPackage)(_), state.readOnly)
          )
        } else EmptyTag
      )
    }

  }

  private[registry] val component = ReactComponentB[Props]("JobForm")
    .initialState(State(new EditableJobSpec(None)))
    .renderBackend[Backend]
    .build

  def apply(handler: Handler, refName: String) =
    component.withRef(refName)(Props(handler))

}
