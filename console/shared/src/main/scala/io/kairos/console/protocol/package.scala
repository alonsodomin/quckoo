package io.kairos.console

package object protocol {

  case class LoginRequest(username: String, password: String)

  case class JobSpecRequest()
  case class JobSpecDetails(id: String, name: String)

  case class ClusterDetails(nodeCount: Int, workerCount: Int)

}
