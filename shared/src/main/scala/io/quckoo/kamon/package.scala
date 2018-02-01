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

package io.quckoo

import cats.effect.IO
import cats.implicits._

import _root_.kamon.Kamon
import _root_.kamon.trace.Span

package object kamon {

  implicit class IOMetrics[A](val self: IO[A]) extends AnyVal {
    def trace(spanName: String): IO[A] = {
      def startSpan: IO[Span] = IO(Kamon.buildSpan(spanName).start())

      def completeSpan(span: Span, result: Either[Throwable, A]): IO[A] = result match {
        case Right(res) => IO(span.finish()) *> IO.pure(res)
        case Left(err) =>
          IO(span.addError(err.getClass.getSimpleName, err).finish()) *> IO.raiseError(err)
      }

      val tracker = for {
        span   <- startSpan
        result <- self.attempt
      } yield (span, result)

      tracker.flatMap { case (span, result) => completeSpan(span, result) }
    }
  }

}
