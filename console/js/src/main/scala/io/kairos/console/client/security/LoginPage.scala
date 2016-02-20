package io.kairos.console.client.security

import io.kairos.console.client.SiteMap.{ConsolePage, Home, Login}
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.layout.{Notification, NotificationDisplay, Panel}
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
  }

  case class NotificationHolder(notifications: Seq[Notification] = Seq())

  class LoginBackend($: BackendScope[RouterCtl[ConsolePage], NotificationHolder]) {
    import LoginForm.LoginInfo

    def loginHandler(loginInfo: LoginInfo): Callback = {

      def authFailedNotification(holder: NotificationHolder): NotificationHolder =
        holder.copy(notifications = holder.notifications :+ Notification.danger("Username or password incorrect"))

      def performLogin(): Future[Callback] =
        ClientApi.login(loginInfo.username, loginInfo.password).map { _ =>
          $.props.flatMap(_.set(Home))
        } recover { case _ =>
          $.modState(holder => authFailedNotification(holder)).
            flatMap(_ => $.props.flatMap(_.set(Login)))
        }

      $.modState(holder => holder.copy(notifications = Seq())) >>
        Callback.future(performLogin())
    }

    def render(holder: NotificationHolder) =
      <.div(Style.formPlacement,
        NotificationDisplay(holder.notifications),
        Panel("Sign in into Kairos Console", LoginForm(loginHandler))
      )
  }

  private[this] val component = ReactComponentB[RouterCtl[ConsolePage]]("LoginPage").
    initialState(NotificationHolder()).
    renderBackend[LoginBackend].
    build

  def apply(router: RouterCtl[ConsolePage]) = component(router)

}
