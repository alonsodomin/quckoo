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

import io.quckoo.{JobPackage, JarJobPackage, ShellScriptPackage}
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

object JobPackageSelect {

  final val Options = List('Jar, 'Shell)

  type Constructor = CoproductSelect.Constructor[JobPackage]
  type Selector    = CoproductSelect.Selector[JobPackage]
  type OnUpdate    = CoproductSelect.OnUpdate[JobPackage]

  final case class Props(value: Option[JobPackage], onUpdate: OnUpdate, readOnly: Boolean = false)

  class Backend($: BackendScope[Props, Unit]) {

    def selectComponent(props: Props): Selector = {
      case 'Jar   => (value, update) =>
        JarJobPackageInput(value.map(_.asInstanceOf[JarJobPackage]), update, props.readOnly)
      case 'Shell => (value, update) =>
        ShellScriptPackageInput(value.map(_.asInstanceOf[ShellScriptPackage]), update, props.readOnly)
    }

    private[this] val selectInput = CoproductSelect[JobPackage] {
      case _: JarJobPackage      => 'Jar
      case _: ShellScriptPackage => 'Shell
    }

    def render(props: Props) = {
      selectInput(
        Options, selectComponent(props), props.value, props.onUpdate,
        ^.id := "packageType", ^.readOnly := props.readOnly, ^.disabled := props.readOnly
      )(
        Seq(<.label(^.`class` := "col-sm-2 control-label", ^.`for` := "packageType", "Package Type"))
      )
    }

  }

  val component = ScalaComponent.builder[Props]("JobPackage")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(value: Option[JobPackage], onUpdate: OnUpdate, readOnly: Boolean = false) =
    component(Props(value, onUpdate, readOnly))

}
