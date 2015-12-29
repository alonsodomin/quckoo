package io.kairos.console.client.security

import io.kairos.console.client.SiteMap.{ConsolePage, Home, Login}
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.layout.{Notification, NotificationDisplay}
import japgolly.scalajs.react.extra.router.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, Callback, ReactComponentB}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scalacss.Defaults._
import scalacss.ScalaCssReact._

/**
 * Created by alonsodomin on 13/10/2015.
 */
object LoginPage {

  object Style extends StyleSheet.Inline {
    import dsl._

    val formPlacement = style(
      width(350 px),
      height(300 px),
      position.absolute,
      left(50 %%),
      top(50 %%),
      marginLeft(-150 px),
      marginTop(-180 px)
    )

    object loginPanel {
      val container = style(addClassNames("panel", "panel-default"))
      val header = style(addClassName("panel-heading"))
      val body = style(addClassName("panel-body"))
    }

    initInnerObjects(loginPanel.container, loginPanel.header, loginPanel.body)
  }

  case class NotificationHolder(notifications: Seq[Notification] = Seq())

  class LoginBackend($: BackendScope[RouterCtl[ConsolePage], NotificationHolder]) {
    import LoginForm.LoginInfo

    def loginHandler(loginInfo: LoginInfo): Callback = {

      def authFailedNotification(holder: NotificationHolder): NotificationHolder =
        holder.copy(notifications = holder.notifications :+ Notification.error("Username or password incorrect"))

      def performLogin(): Future[Callback] =
        ClientApi.login(loginInfo.username, loginInfo.password).map { _ =>
          $.props.flatMap(_.set(Home))
        } recover { case _ =>
          $.modState(holder => authFailedNotification(holder)).
            flatMap(_ => $.props.flatMap(_.set(Login)))
        }

      Callback.future(performLogin())
    }

    def render(holder: NotificationHolder) =
      <.div(Style.formPlacement,
        NotificationDisplay(holder.notifications),
        <.div(Style.loginPanel.container,
          <.div(Style.loginPanel.header, "Sign in into Kairos Console"),
          <.div(Style.loginPanel.body, LoginForm(loginHandler))
        )
      )
  }

  private[this] val component = ReactComponentB[RouterCtl[ConsolePage]]("LoginPage").
    initialState(NotificationHolder()).
    renderBackend[LoginBackend].
    build

  def apply(router: RouterCtl[ConsolePage]) = component(router)

}
