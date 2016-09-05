package io.quckoo.client.internal.channel

import scala.language.higherKinds

/**
  * Created by alonsodomin on 05/09/2016.
  */
trait Channel[Out] {
  type M[_]

}
