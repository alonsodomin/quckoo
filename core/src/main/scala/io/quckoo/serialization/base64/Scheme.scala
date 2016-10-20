package io.quckoo.serialization.base64

import scala.collection.immutable.HashMap

/**
  * Created by alonsodomin on 20/10/2016.
  */
class Scheme(val encodeTable: IndexedSeq[Char]) {
  lazy val decodeTable = HashMap(encodeTable.zipWithIndex: _*)
}
