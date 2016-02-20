package io.kairos.console.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.jquery._

import scala.scalajs.js
import scalacss.ScalaCssReact._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object Modal {
  case class Props(header: Callback => ReactNode,
                   footer: Callback => ReactNode,
                   closed: Callback,
                   backdrop: Boolean = true,
                   keyboard: Boolean = true)

  class Backend($: BackendScope[Props, Unit]) {

    def hide = Callback {
      jQuery($.getDOMNode()).modal("hide")
    }

    def hidden(e: JQueryEventObject): js.Any = {
      $.props.flatMap(_.closed).runNow()
    }

    def render(p: Props, c: PropsChildren) = {
      val modalStyle = lookAndFeel.modal
      <.div(modalStyle.modal, modalStyle.fade, ^.role := "dialog", ^.aria.hidden := true,
        <.div(modalStyle.dialog,
          <.div(modalStyle.content,
            <.div(modalStyle.header, p.header(hide)),
            <.div(modalStyle.body, c),
            <.div(modalStyle.footer, p.footer(hide))
          )
        )
      )
    }
  }

  val component = ReactComponentB[Props]("Modal").
    renderBackend[Backend].
    componentDidMount(scope => Callback {
      val p = scope.props
      // instruct Bootstrap to show the modal
      jQuery(scope.getDOMNode()).modal(
        js.Dynamic.literal(
          "backdrop" -> p.backdrop,
          "keyboard" -> p.keyboard,
          "show"     -> true
        )
      )

      // register event listener to be notified when the modal is closed
      jQuery(scope.getDOMNode()).on("hidden.bs.modal", null, null, scope.backend.hidden _)
    }).build

  def apply() = component
  def apply(props: Props, children: ReactNode*) = component(props, children: _*)
}
