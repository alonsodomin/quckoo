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

  object Style extends Enumeration {
    val default, primary, success, info, warning, danger = Value
  }

  class LookAndFeel(implicit r: Register) extends StyleSheet.Inline()(r) {
    import Style._
    import dsl._

    val global = Domain.ofValues(default, primary, success, info, warning, danger)
    val context = Domain.ofValues(success, info, warning, danger)

    def common[A](domain: Domain[A], base: String) = styleF(domain) { opt =>
      styleS(addClassNames(base, s"$base-$opt"))
    }

    def wrap(classNames: String*) = style(addClassNames(classNames: _*))

    val buttonOpt = common(global, "btn")
    val button    = buttonOpt(default)

    val panelOpt     = common(global, "panel")
    val panel        = panelOpt(default)
    val panelHeading = wrap("panel-heading")
    val panelBody    = wrap("panel-body")

    val labelOpt = common(global, "label")
    val label    = labelOpt(default)

    val alert    = common(global, "alert")

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

  object Button {
    case class Props(
        onClick: Option[Callback] = None,
        style: Style.Value = Style.default,
        addStyles: Seq[StyleA] = Seq()
    )

    val component = ReactComponentB[Props]("Button").
      renderPC { (_, p, c) =>
        val mods = p.onClick.map(callback => ^.onClick --> callback)
        val buttonType = if (p.onClick.isEmpty) "submit" else "button"
        <.button(lookAndFeel.buttonOpt(p.style), p.addStyles, ^.tpe := buttonType, mods.toList, c)
      } build

    def apply() = component
    def apply(props: Props, children: ReactNode*) = component(props, children: _*)
  }

  object Panel {
    case class Props(heading: String, style: Style.Value = Style.default)

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
