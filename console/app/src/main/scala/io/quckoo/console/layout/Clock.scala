package io.quckoo.console.layout

import io.quckoo.time.{DateTime, TimeSource}

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra.TimerSupport
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 04/07/2016.
  */
object Clock {

  final case class State(dateTime: DateTime)

  class Backend($: BackendScope[TimeSource, State]) extends TimerSupport {

    protected[Clock] def mounted(timeSource: TimeSource) =
      setInterval(tick(timeSource), 1 second)

    def tick(timeSource: TimeSource): Callback =
      $.modState(_.copy(dateTime = timeSource.currentDateTime))

    def render(timeSource: TimeSource, state: State) = {
      <.span(state.dateTime.format("dddd, MMMM Do YYYY, h:mm:ss a"))
    }

  }

  private[this] val component = ReactComponentB[TimeSource]("Clock").
    initialState_P(ts => State(ts.currentDateTime)).
    renderBackend[Backend].
    componentDidMount($ => $.backend.mounted($.props)).
    configure(TimerSupport.install).
    build

  def apply(implicit timeSource: TimeSource) = component.withKey("clock")(timeSource)

}
