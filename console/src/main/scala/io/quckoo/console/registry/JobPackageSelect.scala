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
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import enumeratum.values._

import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 16/02/2017.
  */
object JobPackageSelect {
  @inline private def lnf = lookAndFeel

  type OnUpdate = Option[JobPackage] => Callback

  sealed abstract class PackageType(val value: Int, val name: String) extends IntEnumEntry
  object PackageType extends IntEnum[PackageType] {
    case object Jar extends PackageType(1, "Jar")
    case object Shell extends PackageType(2, "Shell")

    val values = findValues
  }
  implicit val packageTypeReuse: Reusability[PackageType] = Reusability.by(_.value)

  private[this] val PackageTypeOption = ReactComponentB[PackageType]("PackageTypeOption")
    .stateless.render_P { packageType =>
      <.option(^.value := packageType.value.toString, packageType.name)
    } build

  private[this] def packageTypeOptions: Seq[ReactElement] =
    PackageType.values.map(pckg => PackageTypeOption.withKey(pckg.value)(pckg))

  case class Props(value: Option[JobPackage], onUpdate: OnUpdate)
  case class State(selected: Option[PackageType], value: Option[JobPackage]) {

    def this(jobPackage: Option[JobPackage]) = this(jobPackage.map {
      case _: JarJobPackage => PackageType.Jar
      case _: ShellScriptPackage => PackageType.Shell
    }, jobPackage)

  }

  class Backend($: BackendScope[Props, State]) {

    private[this] def propagateUpdate: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.value)))

    def onSelectionUpdate(event: ReactEventI): Callback = {
      def refreshSelection: CallbackTo[Option[PackageType]] = {
        val newSelected = {
          if (event.target.value.isEmpty) None
          else Some(event.target.value.toInt)
        }.flatMap(PackageType.withValueOpt)

        $.modState(_.copy(selected = newSelected)).ret(newSelected)
      }

      def updatePackageDetails(packageType: Option[PackageType]): Callback = {
        $.modState(_.copy(value = None), propagateUpdate)
      }

      refreshSelection >>= updatePackageDetails
    }

    def onJobPackageUpdate(jobPackage: Option[JobPackage]): Callback =
      $.modState(_.copy(value = jobPackage), propagateUpdate)

    def render(props: Props, state: State) = {
      <.div(
        <.div(lnf.formGroup,
          <.label("Package Type"),
          <.select(^.`class` := "form-control",
            ^.id := "packageType",
            state.selected.map(v => ^.value := v.value.toString),
            ^.onChange ==> onSelectionUpdate,
            <.option("Select a package type"),
            packageTypeOptions
          )
        ),
        state.selected.flatMap {
          case PackageType.Jar => Some(JarJobPackageInput(state.value.map(_.asInstanceOf[JarJobPackage]), onJobPackageUpdate))
          case _               => None
        }
      )
    }

  }

  val component = ReactComponentB[Props]("JobPackageSelect")
    .initialState_P(props => new State(props.value))
    .renderBackend[Backend]
    .build

  def apply(value: Option[JobPackage], onUpdate: OnUpdate) =
    component(Props(value, onUpdate))

}
