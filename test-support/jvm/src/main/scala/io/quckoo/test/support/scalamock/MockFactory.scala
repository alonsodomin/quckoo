package io.quckoo.test.support.scalamock

import org.scalamock.clazz.Mock
import org.scalatest.TestSuite

/**
  * Created by alonsodomin on 04/09/2016.
  */
trait MockFactory extends AbstractMockFactory with Mock with TestSuite
