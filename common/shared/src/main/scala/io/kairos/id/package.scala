package io.kairos

import java.util.UUID
import derive.key

/**
  * Created by alonsodomin on 14/03/2016.
  */
package object id {

  @key("PlanId") type PlanId = UUID
  @key("TaskId") type TaskId = UUID

}
