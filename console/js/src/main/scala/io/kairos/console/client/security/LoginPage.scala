package io.kairos.console.client.security

import io.kairos.console.client.SiteMap.{ConsolePage, Home, Login}
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.layout.{Notification, NotificationDisplay}
import japgolly.scalajs.react.extra.router2.RouterCtl
import japgolly.scalajs.react.vdom.prefix_<^._
import japgolly.scalajs.react.{BackendScope, ReactComponentB}

import scala.concurrent.Future
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalaz.effect.IO

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

    def loginHandler(loginInfo: LoginInfo): Unit = {
      val router = $.props

      def authFailedNotification(notifs: Seq[Notification]): Seq[Notification] =
        notifs :+ Notification(Notification.Level.Error, "Username or password incorrect")

      def performLogin(): Future[IO[Unit]] =
        ClientApi.login(loginInfo.username, loginInfo.password).map { _ =>
          router.set(Home)
        } recover { case _ =>
          router.set(Login).map { _ =>
            $.modState(s => s.copy(notifications = authFailedNotification(s.notifications)))
          }
        }

      performLogin() onSuccess { case io => io.unsafePerformIO() }
    }

  }

  private[this] val component = ReactComponentB[RouterCtl[ConsolePage]]("LoginPage").
    initialState(NotificationHolder()).
    backend(new LoginBackend(_)).
    render((props, s, b) => {
      <.div(Style.formPlacement,
        NotificationDisplay(s.notifications),
        <.div(Style.loginPanel.container,
          <.div(Style.loginPanel.header, "Sign in into Kairos Console"),
          <.div(Style.loginPanel.body, LoginForm(b.loginHandler))
        )
      )
    }
  ).build

  def apply(router: RouterCtl[ConsolePage]) = component(router)

}
