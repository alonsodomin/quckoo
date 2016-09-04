package io.quckoo.test.support

import org.scalatest.exceptions.StackDepthException

/**
  * Created by alonsodomin on 04/09/2016.
  */
package object scalamock {

  private[scalamock] def failedCodeStackDepthFn(methodName: Option[Symbol]): StackDepthException => Int = e => {
    e.getStackTrace indexWhere { s =>
      !s.getClassName.startsWith("org.scalamock") && !s.getClassName.startsWith("org.scalatest") &&
        !(s.getMethodName == "newExpectationException") && !(s.getMethodName == "reportUnexpectedCall") &&
        !(methodName.isDefined && s.getMethodName == methodName.get.name)
    }
  }

}
