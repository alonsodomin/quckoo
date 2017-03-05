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

package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scalacss.ScalaCssReact._

object CoproductSelect {
  @inline private def lnf = lookAndFeel

  private[this] val ComponentOption = ReactComponentB[Symbol]("ComponentOption")
    .stateless.render_P { sym =>
      <.option(^.value := sym.name, sym.name)
    } build

  type OnUpdate[A]    = Option[A] => Callback
  type Constructor[A] = (Option[A], OnUpdate[A]) => ReactNode
  type Selector[A]    = PartialFunction[Symbol, Constructor[A]]
  type ValueMapper[A] = PartialFunction[A, Symbol]

  final case class Props[A](
    options: List[Symbol],
    selector: Selector[A],
    value: Option[A],
    default: Option[Symbol],
    onUpdate: OnUpdate[A],
    attrs: Seq[TagMod]
  )
  final case class State[A: Reusability](
    selected: Option[Symbol] = None,
    cache: Map[Symbol, A] = Map.empty[Symbol, A]
  )

  implicit def propsReuse[A: Reusability]: Reusability[Props[A]] =
    Reusability.caseClassExcept[Props[A]]('selector, 'onUpdate, 'attrs)
  implicit def cacheReuse[A: Reusability]: Reusability[Map[Symbol, A]] =
    Reusability.map[Symbol, A]
  implicit def stateReuse[A: Reusability]: Reusability[State[A]] =
    Reusability.caseClass[State[A]]

  class Backend[A: Reusability]($: BackendScope[Props[A], State[A]]) {

    private[this] def propagateUpdate: Callback =
      $.state.flatMap(st => $.props.flatMap(_.onUpdate(st.selected.flatMap(st.cache.get))))

    def onSelectionUpdate(props: Props[A])(evt: ReactEventI): Callback = {
      val selectedSymbol: Option[Symbol] = {
        if (evt.target.value.isEmpty) None
        else Some(Symbol(evt.target.value))
      }

      $.modState(_.copy(selected = selectedSymbol), propagateUpdate)
    }

    def onItemUpdate(value: Option[A]): Callback = {
      def updatedCache(selection: Symbol, cache: Map[Symbol, A]) =
        value.map(v => cache + (selection -> v)).getOrElse(cache - selection)

      $.state.map(_.selected).flatMap {
        case Some(selection) =>
          $.modState(st => st.copy(cache = updatedCache(selection, st.cache)), propagateUpdate)

        case None => propagateUpdate
      }
    }

    def render(props: Props[A], children: PropsChildren, state: State[A]) = {
      <.div(
        <.div(lnf.formGroup,
          children,
          <.div(^.`class` := "col-sm-10",
            <.select(lnf.formControl,
              ^.value := state.selected.orElse(props.default).map(_.name).getOrElse(""),
              ^.onChange ==> onSelectionUpdate(props),
              props.attrs,
              if (props.default.isEmpty) {
                <.option("Choose one")
              } else EmptyTag,
              props.options.map(opt => ComponentOption.withKey(opt.name)(opt))
            )
          )
        ),
        state.selected.flatMap { selection =>
          val ctor = props.selector.lift(selection)
          ctor.map(_(state.cache.get(selection), onItemUpdate))
        }
      )
    }

  }

  def apply[A: Reusability](mapper: ValueMapper[A]) = new CoproductSelect[A](mapper)

}

class CoproductSelect[A: Reusability] private[components](mapper: CoproductSelect.ValueMapper[A]) {
  import CoproductSelect._

  private def generateState(props: Props[A], cache: Map[Symbol, A] = Map.empty): State[A] = {
    val selectedSymbol = props.value.flatMap(mapper.lift)
    val rebuiltCache   = selectedSymbol.zip(props.value).map {
      case (sym, value) => cache + (sym -> value)
    }.headOption.getOrElse(cache)

    State[A](
      selected = selectedSymbol,
      cache    = rebuiltCache
    )
  }

  private[components] val component = ReactComponentB[Props[A]]("CoproductSelect")
    .initialState_P(generateState(_))
    .renderBackend[Backend[A]]
    .configure(Reusability.shouldComponentUpdate[Props[A], State[A], Backend[A], TopNode])
    .build

  def apply(options: List[Symbol], selector: Selector[A], value: Option[A], default: Symbol, onUpdate: OnUpdate[A], attrs: TagMod*)(children: ReactNode*) =
    component(Props(options, selector, value, Some(default), onUpdate, attrs), children: _*)

  def apply(options: List[Symbol], selector: Selector[A], value: Option[A], onUpdate: OnUpdate[A], attrs: TagMod*)(children: ReactNode*) =
    component(Props(options, selector, value, None, onUpdate, attrs), children: _*)
}
