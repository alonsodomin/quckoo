package io.chronos.multijvm

import akka.remote.testkit.MultiNodeSpecCallbacks

trait ScalaTestMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with BeforeAndAfterAll {

  override def beforeAll() = multiNodeSpecBeforeAll()

  override def afterAll() = multiNodeSpecAfterAll()

}