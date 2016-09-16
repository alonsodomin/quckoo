package io.quckoo.test.support

import org.scalatest.exceptions.StackDepthException

/**
  * Created by alonsodomin on 04/09/2016.
  */
package object scalamock {

  private[this] final val InternalPackages = List("org.scalamock", "org.scalatest", this.getClass.getPackage.getName)

  private[scalamock] def failedCodeStackDepthFn(methodName: Option[Symbol]): StackDepthException => Int = e => {
    e.getStackTrace indexWhere { s =>
      !InternalPackages.exists(pck => s.getClassName.startsWith(pck)) &&
        !(s.getMethodName == "newExpectationException") && !(s.getMethodName == "reportUnexpectedCall") &&
        !(methodName.isDefined && s.getMethodName == methodName.get.name)
    }
  }

}
