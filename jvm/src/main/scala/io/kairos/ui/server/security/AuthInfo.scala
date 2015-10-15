package io.kairos.ui.server.security

import io.kairos.ui.auth.UserId

/**
 * Created by alonsodomin on 14/10/2015.
 */
class AuthInfo(val userId: UserId, val token: String) {

  def hasPermission(permission: Permission): Boolean = ???

  def copy(token: String): AuthInfo =
    new AuthInfo(userId, token)

}
