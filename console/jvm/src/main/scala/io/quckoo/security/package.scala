package io.quckoo

import java.util.UUID

/**
  * Created by alonsodomin on 21/02/2016.
  */
package object security {

  def generateAuthToken = UUID.randomUUID().toString

}
