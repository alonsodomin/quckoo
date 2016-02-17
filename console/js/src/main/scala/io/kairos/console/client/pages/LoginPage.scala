package io.kairos.console.client.pages

import io.kairos.console.client.boot.Routees
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.security.ClientAuth
import org.scalajs.dom
import org.widok._
import org.widok.bindings.Bootstrap._
import org.widok.bindings.{FontAwesome => fa, HTML}
import pl.metastack.metarx.Var

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
  * Created by alonsodomin on 17/02/2016.
  */
case class LoginPage() extends KairosPage with ClientAuth {
  import KairosPage._

  val username = Var("")
  val password = Var("")

  private[this] val loggedIn = Var(false)
  loggedIn.attach(li => if(li) Routees.home().go())

  private[this] val submitButton = Button(fa.SignIn(), "Sign in").style(Style.Primary)

  whenReady { _ =>
    if (isAuthenticated) {
      Routees.home().go()
      ReadyResult.ShortCircuit
    } else ReadyResult.Continue
  }

  override def header: Widget[_] = HTML.Raw("")

  def submitForm(event: dom.Event) = {
    ClientApi.login(username.get, password.get) onComplete {
      case Success(_) => loggedIn := true
      case Failure(_) =>
    }
  }

  override def body(route: InstantiatedRoute): View = {
    val container = HTML.Container.Generic(
      Panel(
        Panel.Heading("Sign in to Kairos"),
        Panel.Body(
          HTML.Form(
            FormGroup(
              ControlLabel("Username").forId("username"),
              Input.Text().id("username").placeholder("Username").bind(username)
            ),
            FormGroup(
              ControlLabel("Password").forId("password"),
              Input.Password().id("password").placeholder("Password").bind(password)
            ),
            submitButton
          ).id("loginForm").onSubmit(submitForm)
        )
      ).id("loginPanel")
    )

    container.className.append("center-container")
    container
  }

  override def destroy(): Unit = {
    username.dispose()
    password.dispose()
  }

}
