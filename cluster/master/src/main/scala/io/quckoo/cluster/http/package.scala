package io.quckoo.cluster

import java.util.UUID

/**
  * Created by alonsodomin on 24/03/2016.
  */
package object http {

  def generateAuthToken = UUID.randomUUID().toString

}
