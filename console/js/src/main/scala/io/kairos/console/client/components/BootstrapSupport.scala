package io.kairos.console.client.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._
import org.scalajs.jquery._

import scala.scalajs.js
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.Register

/**
  * Created by alonsodomin on 20/02/2016.
  */
object BootstrapSupport {
  import scala.language.implicitConversions

  @js.native
  trait BootstrapJQuery extends JQuery {
    def modal(action: String): BootstrapJQuery = js.native
    def modal(options: js.Any): BootstrapJQuery = js.native
  }
  implicit def jq2bootstrap(jq: JQuery): BootstrapJQuery = jq.asInstanceOf[BootstrapJQuery]

  object ContextStyle extends Enumeration {
    val default, primary, success, info, warning, danger = Value
  }

  class LookAndFeel(implicit r: Register) extends StyleSheet.Inline()(r) {
    import ContextStyle._
    import dsl._

    val global = Domain.ofValues(default, primary, success, info, warning, danger)
    val context = Domain.ofValues(success, info, warning, danger)

    def from[A](domain: Domain[A], base: String) = styleF(domain) { opt =>
      styleS(addClassNames(base, s"$base-$opt"))
    }

    def wrap(classNames: String*) = style(addClassNames(classNames: _*))

    val buttonOpt = from(global, "btn")
    val button    = buttonOpt(default)

    val panelOpt     = from(global, "panel")
    val panel        = panelOpt(default)
    val panelHeading = wrap("panel-heading")
    val panelBody    = wrap("panel-body")

    val labelOpt = from(global, "label")
    val label    = labelOpt(default)

    val alert    = from(context, "alert")

    object modal {
      val modal   = wrap("modal")
      val fade    = wrap("fade")
      val dialog  = wrap("modal-dialog")
      val content = wrap("modal-content")
      val header  = wrap("modal-header")
      val body    = wrap("modal-body")
      val footer  = wrap("modal-footer")
    }

  }

  val lookAndFeel = new LookAndFeel

  object Alert {
    case class Props(style: ContextStyle.Value)

    val component = ReactComponentB[Props]("Alert").
      renderPC { (_, p, c) =>
        <.div(lookAndFeel.alert(p.style), ^.role := "alert", ^.padding := 5.px, c)
      } build

    def apply() = component
    def apply(style: ContextStyle.Value, children: ReactNode*) = component(Props(style), children)
  }

  object Button {
    case class Props(
       onClick: Option[Callback] = None,
       style: ContextStyle.Value = ContextStyle.default,
       addStyles: Seq[StyleA] = Seq()
    )

    val component = ReactComponentB[Props]("Button").
      renderPC { (_, p, c) =>
        val buttonType = if (p.onClick.isEmpty) "submit" else "button"
        <.button(lookAndFeel.buttonOpt(p.style), p.addStyles, ^.tpe := buttonType,
          p.onClick.isDefined ?= (^.onClick --> p.onClick.get), c
        )
      } build

    def apply() = component
    def apply(props: Props, children: ReactNode*) = component(props, children: _*)
  }

  object Panel {
    case class Props(heading: String, style: ContextStyle.Value = ContextStyle.default)

    val component = ReactComponentB[Props]("Panel").
      renderPC { (_, p, c) =>
        <.div(lookAndFeel.panelOpt(p.style),
          <.div(lookAndFeel.panelHeading, p.heading),
          <.div(lookAndFeel.panelBody, c)
        )
      } build

    def apply() = component
    def apply(props: Props, children: ReactNode*) = component(props, children: _*)
  }

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

}
