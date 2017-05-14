/*
 * Copyright 2015 A. Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.client.core

import io.quckoo.client.QuckooClient
import io.quckoo.util.Attempt

import org.scalatest._
import org.scalatest.matchers.Matcher

import scala.concurrent.Future

/**
  * Created by alonsodomin on 17/09/2016.
  */
trait StubClient { this: Assertions with Matchers =>

  final class ClientRunner[P <: Protocol](client: QuckooClient[P]) {
    def usingClient(exec: QuckooClient[P] => Future[Assertion]) = exec(client)
  }

  final class RequestClause[P <: Protocol](matcher: Matcher[P#Request])(implicit commands: ProtocolSpecs[P]) {
    def replyWith(process: P#Request => Attempt[P#Response]): ClientRunner[P] = {
      val handleRequest: P#Request => Attempt[P#Response] = { req =>
        OutcomeOf.outcomeOf(req should matcher) match {
          case Succeeded => process(req)
          case Exceptional(ex) => throw ex
        }
      }

      implicit val backend = new TestDriverBackend[P](Seq.empty, handleRequest)
      implicit val driver = Driver[P]

      new ClientRunner(QuckooClient[P])
    }
  }

  final class InProtocolClause[P <: Protocol](implicit commands: ProtocolSpecs[P]) {
    def ensuringRequest(matcher: Matcher[P#Request]) = new RequestClause[P](matcher)
    def withEvents(events: Iterable[P#EventType]): ClientRunner[P] = {
      val requestError = new Exception("Backend should have been used for subscriptions")

      implicit val backend = new TestDriverBackend[P](events, _ => Attempt.fail(requestError))
      implicit val driver = Driver[P]

      new ClientRunner[P](QuckooClient[P])
    }
  }

  final def inProtocol[P <: Protocol](implicit commands: ProtocolSpecs[P]) = new InProtocolClause

}
