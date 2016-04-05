package io.quckoo.console.security

import io.quckoo.auth.User
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object UserMenu {

  private[this] val component = ReactComponentB[User]("UserDisplay").
    render_P { user =>
      <.span(user.id)
    } build

  def apply(user: User) = component(user)

}
