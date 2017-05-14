package io.quckoo.console.core

import diode.{ActionHandler, ModelRW}

/**
  * Created by alonsodomin on 14/05/2017.
  */
abstract class ConsoleHandler[A](modelRW: ModelRW[ConsoleScope, A]) extends ActionHandler[ConsoleScope, A](modelRW)
