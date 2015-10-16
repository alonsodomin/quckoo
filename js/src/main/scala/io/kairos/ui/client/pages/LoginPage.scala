package io.kairos.ui.client.pages

import io.kairos.ui.client.ClientApi
import io.kairos.ui.client.SiteMap.{ConsolePage, Home, Login}
import io.kairos.ui.client.layout.{Notification, NotificationDisplay}
import io.kairos.ui.client.security.LoginForm
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
      width(300 px),
      height(300 px),
      position.absolute,
      left(50 %%),
      top(50 %%),
      marginLeft(-150 px),
      marginTop(-150 px)
    )
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
      <.div(
        NotificationDisplay(s.notifications),
        <.div(Style.formPlacement,
          <.div(^.`class` := "panel panel-default",
            <.div(^.`class` := "panel-heading", "Sign in into Kairos Console"),
            <.div(^.`class` := "panel-body", LoginForm(b.loginHandler))
          )
        )
      )
    }
  ).build

  def apply(router: RouterCtl[ConsolePage]) = component(router)

}
