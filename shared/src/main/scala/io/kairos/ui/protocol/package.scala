package io.kairos.ui

package object protocol {

  case class LoginRequest(username: String, password: String)
  case class LoginResponse(token: String)

  case class ClusterDetails()

}
