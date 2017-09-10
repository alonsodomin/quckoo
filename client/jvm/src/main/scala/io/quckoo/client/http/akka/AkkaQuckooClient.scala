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

package io.quckoo.client.http.akka

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Materializer}

import cats.Eval
import cats.effect.IO

import io.quckoo.client.QuckooClient2

import scala.concurrent.ExecutionContext

object AkkaQuckooClient {

  def apply(host: String, port: Int): QuckooClient2 = {
    implicit val actorSystem: ActorSystem = ActorSystem("QuckooClient")
    new AkkaQuckooClient(host, port)
  }

}

class AkkaQuckooClient private (host: String, port: Int)(implicit val actorSystem: ActorSystem)
    extends QuckooClient2 with AkkaHttpClientSupport with AkkaHttpSecurity with AkkaHttpCluster
    with AkkaHttpRegistry with AkkaHttpScheduler {

  override protected lazy val client: HttpClient = Http().outgoingConnection(host, port)

  override implicit lazy val executionContext: ExecutionContext = actorSystem.dispatcher

  override implicit lazy val materializer: Materializer =
    ActorMaterializer(ActorMaterializerSettings(actorSystem), "quckoo-http")

  override def shutdown(): IO[Unit] =
    IO.fromFuture(Eval.later(actorSystem.terminate())).map(_ => ())

}
