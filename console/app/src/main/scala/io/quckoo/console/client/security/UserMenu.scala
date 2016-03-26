package io.quckoo.console.client.security

import diode.react.ModelProxy
import io.quckoo.auth.User
import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 20/02/2016.
  */
object UserMenu {

  private[this] val component = ReactComponentB[ModelProxy[Option[User]]]("UserDisplay").
    render_P { proxy =>
      <.div(proxy.value.map(user => <.span(user.name)).getOrElse(EmptyTag))
    } build

  def apply(proxy: ModelProxy[Option[User]]) = component(proxy)

}
