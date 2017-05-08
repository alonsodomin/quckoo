package io.quckoo.console.log

import cats.effect.IO

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.html_<^._

import monix.execution.{Ack, Cancelable}
import monix.reactive.{Observable, Observer}

import scala.concurrent.Future

/**
  * Created by alonsodomin on 06/05/2017.
  */
object LogDisplay {

  case class Props(logStream: Observable[LogRecord])
  case class State(buffer: List[LogRecord])

  class Backend($ : BackendScope[Props, State]) {
    private var subscription: Option[Cancelable] = Option.empty

    private[LogDisplay] def initialize(props: Props): Callback = Callback {
      val subscriptionRef = props.logStream.subscribe(new Observer[LogRecord] {
        override def onError(ex: Throwable): Unit = ()

        override def onComplete(): Unit = ()

        override def onNext(elem: LogRecord): Future[Ack] =
          appendRecord(elem).map(_ => Ack.Continue).unsafeToFuture()

      })
      subscription = Some(subscriptionRef)
    }

    private[LogDisplay] def dispose(): Callback = Callback {
      subscription.foreach(_.cancel())
      subscription = None
    }

    private[this] def appendRecord(record: LogRecord): IO[Unit] = IO {
      $.modState(st => st.copy(buffer = record :: st.buffer)).runNow()
    }

    def render(props: Props, state: State) = {
      val log = state.buffer.map(_.toString).mkString("\n")

      <.pre(
        ^.border  := "solid 1px black",
        ^.width   := "90ex",
        ^.height  := "20em",
        ^.padding := "2px 6px",
        log
      )
    }

  }

  val component = ScalaComponent.builder[Props]("ConsoleLog")
    .initialState(State(List.empty))
    .renderBackend[Backend]
    .componentDidMount($ => $.backend.initialize($.props))
    .componentWillUnmount(_.backend.dispose())
    .build

  def apply(logStream: Observable[LogRecord]) =
    component(Props(logStream))

}
