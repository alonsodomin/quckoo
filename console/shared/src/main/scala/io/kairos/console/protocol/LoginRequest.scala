package io.kairos.console.protocol

import monocle.macros.Lenses

/**
  * Created by alonsodomin on 23/12/2015.
  */
@Lenses
case class LoginRequest(username: String, password: String)
case object LogoutRequest
