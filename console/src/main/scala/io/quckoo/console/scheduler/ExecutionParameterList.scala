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

package io.quckoo.console.scheduler

import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import monocle.function.all._
import monocle.macros.Lenses
import monocle.std.vector._

/**
  * Created by alonsodomin on 09/04/2016.
  */
object ExecutionParameterList {
  import MonocleReact._

  @Lenses case class Param(name: String, value: String)

  case class ParamRowProps(value: Param, onUpdate: Param => Callback, onDelete: Callback)
  case class ParamRowState(name: String, value: String)

  class ParamRowBackend($ : BackendScope[ParamRowProps, ParamRowState]) {

    def propagateChange: Callback = {
      val param = $.state.map(st => Param(st.name, st.value))
      param.flatMap(p => $.props.flatMap(_.onUpdate(p)))
    }

    def onParamNameUpdate(evt: ReactEventFromInput): Callback =
      $.modState(_.copy(name = evt.target.value), propagateChange)

    def onParamValueUpdate(evt: ReactEventFromInput): Callback =
      $.modState(_.copy(value = evt.target.value), propagateChange)

    def render(props: ParamRowProps, state: ParamRowState) = {
      <.div(
        <.div(
          ^.`class` := "col-sm-5",
          <.input.text(
            ^.`class` := "form-control",
            ^.value := state.name,
            ^.onChange ==> onParamNameUpdate)),
        <.div(
          ^.`class` := "col-sm-5",
          <.input.text(
            ^.`class` := "form-control",
            ^.value := state.value,
            ^.onChange ==> onParamValueUpdate)),
        <.div(
          ^.`class` := "col-sm-2",
          Button(
            Button.Props(Some(props.onDelete), style = ContextStyle.default),
            Icons.minus.noPadding))
      )
    }

  }

  val ParamRow = ScalaComponent.builder[ParamRowProps]("ParamRow")
    .initialState_P(props => ParamRowState(props.value.name, props.value.value))
    .renderBackend[ParamRowBackend]
    .build

  case class Props(value: Map[String, String], onUpdate: (String, String) => Callback)
  @Lenses case class State(params: Vector[Param])

  class Backend($ : BackendScope[Props, State]) {

    def addParam(): Callback =
      $.modState(st => st.copy(params = st.params :+ Param("", "")))

    def deleteParam(idx: Int): Callback = {
      def removeIdxFromVector(ps: Vector[Param]): Vector[Param] =
        ps.take(idx) ++ ps.drop(idx + 1)

      $.modState(st => State.params.modify(removeIdxFromVector)(st))
    }

    def onParamUpdate(idx: Int)(param: Param): Callback = {
      val indexLens = State.params ^|-? index(idx)

      $.setStateL(indexLens)(param) >> $.props.flatMap(_.onUpdate(param.name, param.value))
    }

    def render(props: Props, state: State) = {
      <.div(
        <.div(
          ^.`class` := "form-group",
          <.label(^.`class` := "col-sm-2 control-label", "Parameters"),
          <.div(
            ^.`class` := "col-sm-10",
            Button(
              Button.Props(Some(addParam()), style = ContextStyle.default),
              Icons.plus.noPadding))),
        if (state.params.isEmpty) EmptyVdom
        else {
          <.div(
            <.div(
              ^.`class` := "col-sm-offset-2",
              <.div(^.`class` := "col-sm-5", <.label(^.`class` := "control-label", "Name")),
              <.div(^.`class` := "col-sm-5", <.label(^.`class` := "control-label", "Value"))),
            state.params.zipWithIndex.map {
              case (param, idx) =>
                <.div(
                  ^.`class` := "col-sm-offset-2",
                  ParamRow.withKey(idx)(
                    ParamRowProps(param, onParamUpdate(idx), deleteParam(idx))))
            } toVdomArray
          )
        }
      )
    }

  }

  val component = ScalaComponent.builder[Props]("ParameterList")
    .initialState_P(props => State(Vector.empty))
    .renderBackend[Backend]
    .build

  def apply(value: Map[String, String], onUpdate: (String, String) => Callback) =
    component(Props(value, onUpdate))

}
