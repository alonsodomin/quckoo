package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

import monifu.concurrent.Implicits.globalScheduler
import monifu.concurrent.Cancelable
import monifu.reactive.Ack.Continue
import monifu.reactive.Observable

/**
  * Created by alonsodomin on 21/02/2016.
  */
object FormGroup {

  case class Props(observable: Observable[Boolean])
  case class State(valid: Boolean = true)

  class Backend($: BackendScope[Props, State]) {
    private var subscription: Cancelable = _

    def updateValid(v: Boolean) = {
      $.modState(_.copy(valid = v))
    }

    def subscribe = $.props.map(p => {
      subscription = p.observable.subscribe(v => {
        this.updateValid(v).runNow()
        Continue
      })
    })

    def cancel = subscription.cancel()

  }

  private[this] val component = ReactComponentB[Props]("FormGroup").
    initialState(State()).
    backend(new Backend(_)).
    renderCS((_, children, state) => {
      <.div(^.classSet1("form-group", "has-error" -> !state.valid),
        children
      )
    }).
    componentDidMount(_.backend.subscribe).
    componentWillUnmount($ => CallbackTo.pure($.backend.cancel)).
    build

  def apply(observable: Observable[Boolean], children: ReactNode*) =
    component(Props(observable), children: _*)

}
