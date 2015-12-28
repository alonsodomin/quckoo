package io.kairos.cluster

import io.kairos.id._

/**
 * Created by aalonsodominguez on 05/07/15.
 */

case class Task(id: TaskId,
                artifactId: ArtifactId,
                params: Map[String, AnyVal] = Map.empty,
                jobClass: String)
