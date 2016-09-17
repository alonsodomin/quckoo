package io.quckoo.client.http

import upickle.default.{Reader => UReader}

import io.quckoo.auth.Passport
import io.quckoo.serialization.DataBuffer

import org.scalatest.Matchers
import org.scalatest.matchers.{MatchResult, Matcher}

import scala.util.matching.Regex

/**
  * Created by alonsodomin on 17/09/2016.
  */
trait HttpRequestMatchers extends Matchers {

  def hasMethod(method: HttpMethod): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult =
      MatchResult(req.method == method,
        s"Http method ${req.method} is not a $method method",
        s"is a ${req.method} http request"
      )
  }

  def hasUrl(pattern: Regex): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult =
      MatchResult(pattern.findAllIn(req.url).nonEmpty,
        s"URL '${req.url}' does not match pattern '$pattern'",
        s"URL '${req.url}' matches pattern '$pattern'"
      )
  }

  def hasAuthHeader(username: String, password: String): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult = {
      val creds = DataBuffer.fromString(s"$username:$password").toBase64
      MatchResult(
        req.headers.get(AuthorizationHeader).contains(s"Basic $creds"),
        s"no '$AuthorizationHeader' header for '$username' with password '$password' was found in the request",
        s"'$AuthorizationHeader' header has the expected value"
      )
    }
  }

  def hasPassport(passport: Passport): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult = {
      MatchResult(
        req.headers.get(AuthorizationHeader).contains(s"Bearer ${passport.token}"),
        s"no '$AuthorizationHeader' header with passport '${passport.token}' was found in the request",
        s"'$AuthorizationHeader' header has the expected value"
      )
    }
  }

  val hasEmptyBody: Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult = {
      MatchResult(
        req.entity.isEmpty,
        s"has no empty body",
        s"has empty body"
      )
    }
  }

  def hasBody[A: UReader](body: A): Matcher[HttpRequest] = new Matcher[HttpRequest] {
    override def apply(req: HttpRequest): MatchResult = {
      MatchResult(
        req.entity.as[A].toOption.contains(body),
        s"current body '${req.entity.asString()}' is not equals to '$body'",
        s"contains '$body' in the request"
      )
    }
  }

}
