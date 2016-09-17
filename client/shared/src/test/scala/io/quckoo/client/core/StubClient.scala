package io.quckoo.client.core

import io.quckoo.client.QuckooClientV2
import io.quckoo.util.LawfulTry

import org.scalatest._
import org.scalatest.matchers.Matcher

import scala.concurrent.Future

/**
  * Created by alonsodomin on 17/09/2016.
  */
trait StubClient { this: Assertions with Matchers =>

  final class ClientRunner[P <: Protocol](client: QuckooClientV2[P]) {
    def usingClient(exec: QuckooClientV2[P] => Future[Assertion]) = exec(client)
  }

  final class RequestClause[P <: Protocol, Req, Res](transport: TestTransport[P], matcher: Matcher[Req]) {
    def replyWith(process: Req => LawfulTry[Res]): ClientRunner[P] = {
      val handleRequest: Req => LawfulTry[Res] = { req =>
        OutcomeOf.outcomeOf(req should matcher) match {
          case Succeeded => process(req)
          case Exceptional(ex) => throw ex
        }
      }

      implicit val liftedTransport = transport.lift(
        // Ugly but safe cast
        handleRequest.asInstanceOf[transport.protocol.Request => LawfulTry[transport.protocol.Response]]
      )
      new ClientRunner(QuckooClientV2[P])
    }
  }

  final class InProtocolClause[P <: Protocol](protocol: P) {
    val transport = new TestTransport[P](protocol)

    def ensuringRequest(
      matcher: Matcher[transport.protocol.Request]
    ): RequestClause[P, transport.protocol.Request, transport.protocol.Response] =
      new RequestClause[P, transport.protocol.Request, transport.protocol.Response](transport, matcher)
  }

  final def inProtocol[P <: Protocol](protocol: P) = new InProtocolClause(protocol)

}
