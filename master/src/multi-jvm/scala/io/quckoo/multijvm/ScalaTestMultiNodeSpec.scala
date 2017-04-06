package io.quckoo.multijvm

import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

/**
 * Created by aalonsodominguez on 27/08/15.
 */
trait ScalaTestMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with BeforeAndAfterAll with Matchers {

   override protected def beforeAll(): Unit = multiNodeSpecBeforeAll()

   override protected def afterAll(): Unit = multiNodeSpecAfterAll()

 }
