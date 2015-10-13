package io.kairos.ui

import scalacss.Defaults._
import scalacss.ScalaCssReact._
import scalacss.mutable.GlobalRegistry

/**
 * Created by alonsodomin on 13/10/2015.
 */
object AppStyle {

  def load() = {
    GlobalRegistry.register(
      WelcomePage.Style,
      LoginPage.Style
    )
    GlobalRegistry.onRegistration(_.addToDocument)
  }

}
