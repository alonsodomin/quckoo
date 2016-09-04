package io.quckoo.test.support.scalamock

import org.scalamock.MockFactoryBase
import org.scalamock.clazz.Mock
import org.scalatest.exceptions.{StackDepthException, TestFailedException}
import org.scalatest.{SuiteMixin, TestSuite}

/**
  * Created by alonsodomin on 04/09/2016.
  */
trait PathMockFactory extends SuiteMixin with MockFactoryBase with Mock with TestSuite {

  type ExpectationException = TestFailedException

  protected def newExpectationException(message: String, methodName: Option[Symbol]) =
    new TestFailedException((_: StackDepthException) => Some(message), None, failedCodeStackDepthFn(methodName))

  /**
    * Verify all expectations.
    */
  protected def verifyExpectations(): Unit = withExpectations(())
}
