package io.chronos.multijvm

import akka.remote.testkit.MultiNodeSpecCallbacks
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

/**
 * Created by aalonsodominguez on 27/08/15.
 */
trait ScalaTestMultiNodeSpec extends MultiNodeSpecCallbacks with WordSpecLike with BeforeAndAfterAll {

   override def beforeAll() = multiNodeSpecBeforeAll()

   override def afterAll() = multiNodeSpecAfterAll()

 }
