package io.kairos.console.client.pages

import io.kairos.console.client.boot.Routees
import io.kairos.console.client.core.ClientApi
import io.kairos.console.client.security.ClientAuth
import io.kairos.console.client.validation._
import org.scalajs.dom
import org.widok._
import org.widok.bindings.{Bootstrap, FontAwesome => fa, HTML}
import pl.metastack.metarx._

import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.util.{Failure, Success}

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait LoginState {
  import validation._
  import Validations._

  val username = Var("")
  val password = Var("")
  val invalidCredentials = Opt[Boolean]()
  invalidCredentials.attach {
    case Some(false) => Routees.dashboard().go()
    case _ =>
  }

  implicit val validator = Validator(
    username -> Seq(RequiredValidation()),
    password -> Seq(RequiredValidation())
  )

}

case class LoginPage() extends KairosPage with ClientAuth with LoginState {
  import Bootstrap._
  import KairosPage._
  import ValidationBridge._

  private[this] val submitButton = Button(fa.SignIn(), "Sign in").
    style(Style.Primary).
    enabled(validator.valid)

  whenReady { _ =>
    import ReadyResult._

    if (isAuthenticated) {
      Routees.dashboard().go()
      ShortCircuit
    } else Continue
  }

  override def header(route: InstantiatedRoute): Widget[_] = HTML.Raw("")

  def submitForm(event: dom.Event) = {
    ClientApi.login(username.get, password.get) onComplete {
      case Success(_) => invalidCredentials := false
      case Failure(_) => invalidCredentials := true
    }
  }

  override def body(route: InstantiatedRoute): View = {
    val container = HTML.Container.Generic(
      Panel(
        Panel.Heading("Sign in to Kairos"),
        Panel.Body(
          Alert("Invalid username or password").
            style(Style.Danger).
            show(invalidCredentials.contains(true)),
          HTML.Form(
            FormGroup(
              InputGroup(
                ControlLabel("Username").forId("username"),
                Input.Text().id("username").placeholder("Username").bind(username),
                validator.message(username)
              ).validated(username)
            ),
            FormGroup(
              InputGroup(
                ControlLabel("Password").forId("password"),
                Input.Password().id("password").placeholder("Password").bind(password),
                validator.message(password)
              ).validated(password)
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
