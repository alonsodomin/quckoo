package io.chronos.scheduler

import akka.testkit.{ImplicitSender, TestKit}
import org.scalatest.{BeforeAndAfterAll, FlatSpecLike}

/**
 * Created by aalonsodominguez on 18/08/15.
 */
class SchedulerSpec extends TestKit(TestActorSystem("SchedulerSpec")) with ImplicitSender
  with FlatSpecLike with BeforeAndAfterAll {

  override protected def afterAll(): Unit =
    TestKit.shutdownActorSystem(system)

}
