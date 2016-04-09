package io.quckoo.console.scheduler

import io.quckoo.Trigger
import io.quckoo.console.components._

import japgolly.scalajs.react._
import japgolly.scalajs.react.extra._
import japgolly.scalajs.react.vdom.prefix_<^._

import scala.concurrent.duration.FiniteDuration

/**
  * Created by alonsodomin on 08/04/2016.
  */
object AfterTriggerInput {

  case class Props(value: Option[Trigger.After], onUpdate: Option[Trigger.After] => Callback)
  implicit val propsReuse = Reusability.by[Props, Option[Trigger.After]](_.value)

  class Backend($: BackendScope[Props, Unit]) {

    def onUpdate(value: Option[FiniteDuration]) =
      $.props.flatMap(_.onUpdate(value.map(Trigger.After)))

    def render(props: Props): ReactElement =
      <.div(^.`class` := "form-group",
        <.label(^.`class` := "col-sm-2 control-label", "Delay"),
        <.div(^.`class` := "col-sm-10",
          FiniteDurationInput("afterTrigger", props.value.map(_.delay), onUpdate)
        )
      )

  }

  val component = ReactComponentB[Props]("AfterTriggerInput").
    stateless.
    renderBackend[Backend].
    configure(Reusability.shouldComponentUpdate).
    build

  def apply(value: Option[Trigger.After], onUpdate: Option[Trigger.After] => Callback) =
    component(Props(value, onUpdate))

}
