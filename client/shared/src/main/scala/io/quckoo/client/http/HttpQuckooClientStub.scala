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

package io.quckoo.client.http

import java.nio.ByteBuffer

import com.softwaremill.sttp.Uri
import com.softwaremill.sttp.impl.monix.TaskMonadAsyncError
import com.softwaremill.sttp.testing.SttpBackendStub

import io.circe.Decoder

import io.quckoo.api.TopicTag

import monix.eval.Task
import monix.reactive.Observable

object HttpQuckooClientStub {
  type Backend = SttpBackendStub[Task, Observable[ByteBuffer]]

  def apply(buildBackend: Backend => Backend): HttpQuckooClient = {
    val backend = SttpBackendStub[Task, Observable[ByteBuffer]](TaskMonadAsyncError)
    new HttpQuckooClientStub()(buildBackend(backend))
  }

}

final class HttpQuckooClientStub private (implicit backend: HttpQuckooClientStub.Backend)
  extends HttpQuckooClient(Some(Uri("example.org"))) {

  def channel[A](implicit topicTag: TopicTag[A], decoder: Decoder[A]): Observable[A] = ???

}
