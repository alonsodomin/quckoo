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

// /*
//  * Copyright 2015 A. Alonso Dominguez
//  *
//  * Licensed under the Apache License, Version 2.0 (the "License");
//  * you may not use this file except in compliance with the License.
//  * You may obtain a copy of the License at
//  *
//  *     http://www.apache.org/licenses/LICENSE-2.0
//  *
//  * Unless required by applicable law or agreed to in writing, software
//  * distributed under the License is distributed on an "AS IS" BASIS,
//  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//  * See the License for the specific language governing permissions and
//  * limitations under the License.
//  */

// package io.quckoo.client.http

// import io.quckoo.auth.Passport
// import io.quckoo.api.RequestTimeoutHeader
// import io.quckoo.serialization.{DataBuffer, Decoder}

// import org.scalatest.Matchers
// import org.scalatest.matchers.{MatchResult, Matcher}

// import scala.concurrent.duration.FiniteDuration
// import scala.util.matching.Regex

// /**
//   * Created by alonsodomin on 17/09/2016.
//   */
// trait HttpRequestMatchers extends Matchers {

//   def hasMethod(method: HttpMethod): Matcher[HttpRequest] = new Matcher[HttpRequest] {
//     override def apply(req: HttpRequest): MatchResult =
//       MatchResult(req.method == method,
//         s"Http method ${req.method} is not a $method method",
//         s"is a ${req.method} http request"
//       )
//   }

//   def hasUrl(pattern: Regex): Matcher[HttpRequest] = new Matcher[HttpRequest] {
//     override def apply(req: HttpRequest): MatchResult =
//       MatchResult(pattern.findAllIn(req.url).nonEmpty,
//         s"URL '${req.url}' does not match pattern '$pattern'",
//         s"URL '${req.url}' matches pattern '$pattern'"
//       )
//   }

//   def hasHeader(name: String, value: String): Matcher[HttpRequest] = new Matcher[HttpRequest] {
//     override def apply(req: HttpRequest): MatchResult = {
//       val currValue = req.headers.get(name)
//       MatchResult(
//         currValue.contains(value),
//         currValue.map { v =>
//           s"header '$name' with value '$v' did not match value '$value'"
//         } getOrElse {
//           s"no '$name' header found in the request"
//         },
//         s"header '$name' has the correct value"
//       )
//     }
//   }

//   val isJsonRequest: Matcher[HttpRequest] =
//     hasHeader("Content-Type", "application/json")

//   def hasAuth(username: String, password: String): Matcher[HttpRequest] = {
//     val Right(creds) = DataBuffer.fromString(s"$username:$password").toBase64
//     hasHeader(AuthorizationHeader, s"Basic $creds")
//   }

//   def hasPassport(passport: Passport): Matcher[HttpRequest] =
//     hasHeader(AuthorizationHeader, s"Bearer ${passport.token}")

//   def hasTimeout(timeout: FiniteDuration): Matcher[HttpRequest] =
//     hasHeader(RequestTimeoutHeader, timeout.toMillis.toString)

//   val hasEmptyBody: Matcher[HttpRequest] = new Matcher[HttpRequest] {
//     override def apply(req: HttpRequest): MatchResult = {
//       MatchResult(
//         req.entity.isEmpty,
//         s"has no empty body",
//         s"has empty body"
//       )
//     }
//   }

//   def hasBody[A](body: A)(implicit decoder: Decoder[String, A]): Matcher[HttpRequest] = new Matcher[HttpRequest] {
//     override def apply(req: HttpRequest): MatchResult = {
//       MatchResult(
//         req.entity.as[A].toOption.contains(body),
//         s"current body '${req.entity.asString()}' is not equals to '$body'",
//         s"contains '$body' in the request"
//       )
//     }
//   }

// }
