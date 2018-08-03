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

import cats.effect._
import cats.implicits._

import com.softwaremill.sttp.{Uri, SttpBackend}
import com.softwaremill.sttp.asynchttpclient.monix._

import io.circe.Decoder

import io.quckoo.api.TopicTag

import monix.eval.Task
import monix.reactive.{Observable, OverflowStrategy}

object JVMQuckooClient {
  def apply(host: String, port: Int = 80): Resource[IO, HttpQuckooClient] = {
    val baseUri = Uri(host, port)
    val backend = Resource.make(IO(AsyncHttpClientMonixBackend()))(b => IO(b.close()))
    backend.map(b => new JVMQuckooClient(baseUri)(b))
  }
}

final class JVMQuckooClient private (baseUri: Uri)(
    implicit backend: SttpBackend[Task, Observable[ByteBuffer]]
) extends HttpQuckooClient(Some(baseUri)) {

  def channel[A](implicit topicTag: TopicTag[A], decoder: Decoder[A]): Observable[A] = ???

}
