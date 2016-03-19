package io.kairos.serialization.json

import upickle.AttributeTagged

/**
  * Created by alonsodomin on 19/03/2016.
  */
trait JsonSerializer extends AttributeTagged {
  override def tagName: String = "$tag"
}
