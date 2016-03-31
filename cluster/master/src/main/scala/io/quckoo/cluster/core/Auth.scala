package io.quckoo.cluster.core

import akka.http.scaladsl.server.directives.Credentials
import authentikat.jwt.{JsonWebToken, JwtClaimsSet, JwtHeader}
import io.quckoo.auth.User
import io.quckoo.serialization.Base64._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait Auth {

  val Realm = "QuckooRealm"
  val secretKey = "dqwjq0jd9wjd192u4ued9hd0ew".getBytes("UTF-8").toBase64

  def authenticateCreds(credentials: Credentials)(implicit ec: ExecutionContext): Future[Option[User]] = {
    credentials match {
      case p @ Credentials.Provided(identifier) =>
        if (identifier == "admin" && p.verify("password"))
          Future.successful(Some(User(identifier)))
        else Future.successful(None)

      case _ =>
        Future.successful(None)
    }
  }

  def authenticateToken(acceptExpired: Boolean = false)(credentials: Credentials)(implicit ec: ExecutionContext): Future[Option[User]] = {
    credentials match {
      case p @ Credentials.Provided(token) =>
        if (isValidToken(token)) {
          val claims = token match {
            case JsonWebToken(_, clms, _) =>
              clms.asSimpleMap.toOption

            case _ => None
          }
          Future.successful(claims.map(claimSet => User(claimSet("sub"))))
        } else {
          Future.successful(None)
        }

      case _ =>
        Future.successful(None)
    }
  }

  def isValidToken(token: String): Boolean = JsonWebToken.validate(token, secretKey)

  def generateToken(user: User) = {
    val header = JwtHeader("HS256")
    val claimsSet = JwtClaimsSet(Map("sub" -> user.id))

    JsonWebToken(header, claimsSet, secretKey)
  }

}
