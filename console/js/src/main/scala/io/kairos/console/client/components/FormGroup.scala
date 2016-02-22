package io.kairos.console.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import monifu.concurrent.Implicits.globalScheduler
import monifu.reactive.Observable

import org.scalajs.dom

/**
  * Created by alonsodomin on 21/02/2016.
  */
object FormGroup {

  case class Props(observable: Observable[Boolean])
  case class State(valid: Boolean = true)

  class Backend($: BackendScope[Props, State]) {

    def updateValid(v: Boolean) = {
      dom.console.log("Status is valid = " + v)
      $.modState(_.copy(valid = v))
    }

  }

  private[this] val component = ReactComponentB[Props]("FormGroup").
    initialState(State()).
    backend(new Backend(_)).
    renderCS((_, children, state) => {
      <.div(^.classSet1("form-group", "has-error" -> !state.valid),
        children
      )
    }).
    componentDidMount($ => Callback {
      $.props.observable.foreach(valid => $.backend.updateValid(valid).runNow())
    }).
    build

  def apply(observable: Observable[Boolean], children: ReactNode*) =
    component(Props(observable), children: _*)

}
