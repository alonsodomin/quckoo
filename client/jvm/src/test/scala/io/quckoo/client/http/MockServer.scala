package io.quckoo.client.http

import org.mockserver.integration.{ClientAndProxy, ClientAndServer}
import org.mockserver.socket.PortFactory
import org.scalatest.{BeforeAndAfterAll, Outcome, fixture}

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait MockServer extends fixture.FlatSpec with BeforeAndAfterAll {
  type FixtureParam = ClientAndServer

  private[this] var proxy: ClientAndProxy = _

  override protected def beforeAll(): Unit = {
    proxy = ClientAndProxy.startClientAndProxy(PortFactory.findFreePort())
  }

  override protected def afterAll(): Unit = proxy.stop()

  override protected def withFixture(test: OneArgTest): Outcome = {
    val server = ClientAndServer.startClientAndServer(PortFactory.findFreePort())
    try {
      proxy.reset()
      withFixture(test.toNoArgTest(server))
    } finally {
      server.stop()
    }
  }
}
