package io.quckoo.console.core

import teststate.{Exports, ExtScalaJsReact, ExtScalaz}
import teststate.domzipper.sizzle

/**
  * Created by alonsodomin on 06/07/2016.
  */
object ConsoleTestState extends Exports
  with ExtScalaJsReact
  with ExtScalaz
  with sizzle.Exports
