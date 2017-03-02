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

import io.quckoo.{JobPackage, JarJobPackage, ShellScriptPackage}
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

object JobPackageSelect {

  final val Options = List('Jar, 'Shell)

  type Constructor = CoproductSelect.Constructor[JobPackage]
  type Selector    = CoproductSelect.Selector[JobPackage]
  type OnUpdate    = CoproductSelect.OnUpdate[JobPackage]

  final case class Props(value: Option[JobPackage], onUpdate: OnUpdate)

  class Backend($: BackendScope[Props, Unit]) {

    def selectComponent: Selector = {
      case 'Jar   => (value, update) => JarJobPackageInput(value.map(_.asInstanceOf[JarJobPackage]), update)
      case 'Shell => (value, update) => ShellScriptPackageInput(value.map(_.asInstanceOf[ShellScriptPackage]), update)
    }

    val selectInput = CoproductSelect[JobPackage] {
      case _: JarJobPackage      => 'Jar
      case _: ShellScriptPackage => 'Shell
    }

    def render(props: Props) = {
      selectInput(Options, selectComponent, props.value, props.onUpdate,
        ^.id := "packageType"
      )(<.label(^.`class` := "col-sm-2 control-label", ^.`for` := "packageType", "Package Type"))
    }

  }

  val component = ReactComponentB[Props]("JobPackage")
    .stateless
    .renderBackend[Backend]
    .build

  def apply(value: Option[JobPackage], onUpdate: OnUpdate) =
    component(Props(value, onUpdate))

}