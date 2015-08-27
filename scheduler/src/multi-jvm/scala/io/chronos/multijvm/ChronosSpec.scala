package io.chronos.multijvm

import akka.remote.testkit.{MultiNodeConfig, MultiNodeSpec}
import akka.testkit.ImplicitSender

/**
 * Created by domingueza on 26/08/15.
 */
object ChronosNodesConfig extends MultiNodeConfig {
  val scheduler1 = role("scheduler")
  val scheduler2 = role("scheduler")
  val registry1  = role("registry")
  val registry2  = role("registry")
}

class ChronosSpecNode1 extends ChronosSpec
class ChronosSpecNode2 extends ChronosSpec

class ChronosSpec extends MultiNodeSpec(ChronosNodesConfig) with ImplicitSender
  with ScalaTestMultiNodeSpec {

  override def initialParticipants: Int = roles.size

}
