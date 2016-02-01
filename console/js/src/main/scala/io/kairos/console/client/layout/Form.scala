package io.kairos.console.client.layout

import japgolly.scalajs.react._
import japgolly.scalajs.react.vdom.prefix_<^._

/**
  * Created by alonsodomin on 01/02/2016.
  */
object Form {

  type FormHandler[T] = T => Callback

  case class Props[T](id: String, value: T, handler: FormHandler[T])
  case class State[T](value: T, valid: Boolean = false)

  private[this] def component[T] = ReactComponentB[Props[T]]("Form").
    initialState_P(props => State(props.value)).
    noBackend.
    renderPS(($, props, state) => {
      val submitForm = (e: ReactEventI) => {
        preventDefault(e)
        if (state.valid) Some(props.handler(state.value))
        else None
      }
      <.form(^.id := props.id, ^.`class` := "form-horizontal", ^.onSubmit ==>? submitForm,
        $.propsChildren.render,
        <.div(^.`class` := "col-sm-offset-2",
          <.button(^.`class` := "btn btn-primary", !state.valid ?= (^.disabled := true), "Submit")
        )
      )
    }).build

  def apply[T](id: String, value: T, children: ReactElement*)(handler: FormHandler[T]) =
    component(Props[T](id, value, handler))

}
