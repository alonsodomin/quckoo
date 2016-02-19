package io.kairos.console.client.validation

import org.widok._
import org.widok.bindings.HTML
import org.widok.validation.Validator
import pl.metastack.metarx.ReadChannel

/**
  * Created by alonsodomin on 19/02/2016.
  */
object ValidationBridge {

  implicit class RichValidator(validator: Validator) {

    def message[A](field: ReadChannel[A]) = HTML.Container.Inline(
      validator.combinedErrors(field)
    ).cssState(validator.invalid(field), "help-block", "with-errors")

  }

  implicit class ValidatedWidget[T](widget: Widget[T]) {

    def validated[A](field: ReadChannel[A])(implicit validator: Validator) =
      widget.cssState(validator.invalid(field), "has-error")

  }

}
