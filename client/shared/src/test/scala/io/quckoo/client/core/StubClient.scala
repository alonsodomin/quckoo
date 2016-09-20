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

  final class RequestClause[P <: Protocol](matcher: Matcher[P#Request])(implicit commands: ProtocolSpecs[P]) {
    def replyWith(process: P#Request => LawfulTry[P#Response]): ClientRunner[P] = {
      val handleRequest: P#Request => LawfulTry[P#Response] = { req =>
        OutcomeOf.outcomeOf(req should matcher) match {
          case Succeeded => process(req)
          case Exceptional(ex) => throw ex
        }
      }

      implicit val transport = new TestDriverBackend[P](handleRequest)
      implicit val driver = Driver[P]

      new ClientRunner(QuckooClientV2[P])
    }
  }

  final class InProtocolClause[P <: Protocol](implicit commands: ProtocolSpecs[P]) {
    def ensuringRequest(matcher: Matcher[P#Request]) = new RequestClause[P](matcher)
  }

  final def inProtocol[P <: Protocol](implicit commands: ProtocolSpecs[P]) = new InProtocolClause

}
