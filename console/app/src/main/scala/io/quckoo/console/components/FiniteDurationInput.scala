package io.quckoo.console.components

import java.util.concurrent.TimeUnit

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration._

/**
  * Created by alonsodomin on 08/04/2016.
  */
object FiniteDurationInput {

  val SupportedUnits = Seq(
    MILLISECONDS -> "Milliseconds",
    SECONDS      -> "Seconds",
    MINUTES      -> "Minutes",
    HOURS        -> "Hours",
    DAYS         -> "Days"
  )

  case class Props(id: String, value: Option[FiniteDuration], onUpdate: Option[FiniteDuration] => Callback)
  case class State(length: Option[Long], unit: Option[TimeUnit]) {

    def this(duration: Option[FiniteDuration]) =
      this(duration.map(_.length), duration.map(_.unit))

  }

  implicit val propsReuse: Reusability[Props] = Reusability.by(_.value)
  implicit val stateReuse = Reusability.caseClass[State]

  class Backend($: BackendScope[Props, State]) {

    def propagateUpdate: Callback = {
      val prod = $.state.map(st => st.length.flatMap(l => st.unit.map(u => (l, u))))

      prod.flatMap {
        case Some((length, unit)) =>
          val duration = FiniteDuration(length, unit)
          $.props.flatMap(_.onUpdate(Some(duration)))

        case _ =>
          $.props.flatMap(_.onUpdate(None))
      }
    }

    def onLengthUpdate(value: Option[Long]): Callback =
      $.modState(_.copy(length = value), propagateUpdate)

    def onUnitUpdate(evt: ReactEventI): Callback = {
      val value = {
        if (evt.target.value.isEmpty) None
        else Some(TimeUnit.valueOf(evt.target.value))
      }
      $.modState(_.copy(unit = value), propagateUpdate)
    }

    val lengthInput = new Input[Long](onLengthUpdate)

    def render(props: Props, state: State) = {
      val id = props.id
      <.div(^.`class` := "container-fluid",
        <.div(^.`class` := "row",
          <.div(^.`class` := "col-sm-4",
            lengthInput(state.length, ^.id := s"${id}_length")
          ),
          <.div(^.`class` := "col-sm-6",
            <.select(^.id := s"${id}_unit", ^.`class` := "form-control",
              state.unit.map(u => ^.value := u.toString()),
              ^.onChange ==> onUnitUpdate,
              SupportedUnits.map { case (u, text) =>
                <.option(^.value := u.name(), text)
              }
            )
          )
        )
      )
    }

  }

  val component = ReactComponentB[Props]("FiniteDurationInput").
    initialState_P(props => new State(props.value)).
    renderBackend[Backend].
    configure(Reusability.shouldComponentUpdate).
    build

  def apply(id: String, value: Option[FiniteDuration], onUpdate: Option[FiniteDuration] => Callback) =
    component(Props(id, value, onUpdate))

}
