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
import io.quckoo.console.components._
import io.quckoo.id.ArtifactId

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

  type RegisterHandler = Option[JobSpec] => Callback

  final case class Props(spec: Option[JobSpec], handler: RegisterHandler)

  @Lenses final case class EditableJobSpec(
    displayName: Option[String] = None,
    description: Option[String] = None,
    artifactId: Option[ArtifactId] = None,
    jobClass: Option[String] = None
  ) {

    def this(jobSpec: Option[JobSpec]) =
      this(jobSpec.map(_.displayName), jobSpec.flatMap(_.description), jobSpec.map(_.artifactId), jobSpec.map(_.jobClass))

    def valid: Boolean =
      displayName.nonEmpty && artifactId.nonEmpty && jobClass.nonEmpty

  }

  @Lenses final case class State(spec: EditableJobSpec, cancelled: Boolean = true)

  class JobFormBackend($: BackendScope[Props, State]) {

    val displayName = State.spec ^|-> EditableJobSpec.displayName
    val description = State.spec ^|-> EditableJobSpec.description
    val artifact    = State.spec ^|-> EditableJobSpec.artifactId
    val jobClass    = State.spec ^|-> EditableJobSpec.jobClass

    def submitForm(): Callback =
      $.modState(_.copy(cancelled = false))

    def onDisplayNameUpdate(value: Option[String]) =
      $.setStateL(displayName)(value)

    def onDescriptionUpdate(value: Option[String]) =
      $.setStateL(description)(value)

    def onJobClassUpdate(value: Option[String]) =
      $.setStateL(jobClass)(value)

    def formClosed(props: Props, state: State) = {
      val jobSpec: Option[JobSpec] = if (!state.cancelled) {
        for {
          name  <- state.spec.displayName
          art   <- state.spec.artifactId
          clazz <- state.spec.jobClass
        } yield JobSpec(name, state.spec.description, art, clazz)
      } else None

      props.handler(jobSpec)
    }

    val displayNameInput = Input[String](onDisplayNameUpdate)
    val descriptionInput = Input[String](onDescriptionUpdate)
    val jobClassInput    = Input[String](onJobClassUpdate)

    def render(props: Props, state: State) = {
      <.form(^.name := "jobDetails",
        Modal(
          Modal.Props(
            header = hide => <.span(
              <.button(^.tpe := "button", lookAndFeel.close, ^.onClick --> hide, Icons.close),
              <.h4("Register Job")
            ),
            footer = hide => <.span(
              Button(Button.Props(Some(hide), style = ContextStyle.default), "Cancel"),
              Button(Button.Props(
                Some(submitForm() >> hide),
                style = ContextStyle.primary,
                disabled = !state.spec.valid
              ), "Ok")
            ),
            closed = formClosed(props, state)
          ),
          <.div(lnf.formGroup,
            <.label(^.`for` := "displayName", "Display Name"),
            displayNameInput(state.spec.displayName,
              ^.id := "displayName",
              ^.placeholder := "Job's name"
            )
          ),
          <.div(lnf.formGroup,
            <.label(^.`for` := "description", "Description"),
            descriptionInput(state.spec.description,
              ^.id := "description",
              ^.placeholder := "Job's description"
            )
          ),
          <.div(lnf.formGroup,
            <.label("Artifact"),
            ArtifactInput(state.spec.artifactId, $.setStateL(artifact)(_))
          ),
          <.div(lnf.formGroup,
            <.label(^.`for` := "jobClass", "Job Class"),
            jobClassInput(state.spec.jobClass,
              ^.id := "jobClass",
              ^.placeholder := "Fully classified job class name"
            )
          )
        )
      )
    }

  }

  private[this] val component = ReactComponentB[Props]("JobForm").
    initialState_P(p => State(new EditableJobSpec(p.spec))).
    renderBackend[JobFormBackend].
    build

  def apply(spec: Option[JobSpec], handler: RegisterHandler) = component(Props(spec, handler))

}
