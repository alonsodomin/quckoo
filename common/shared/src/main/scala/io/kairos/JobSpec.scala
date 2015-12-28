package io.kairos

import io.kairos.id._

/**
 * Created by aalonsodominguez on 10/07/15.
 */
case class JobSpec(displayName: String,
                   description: String = "",
                   artifactId: ArtifactId,
                   jobClass: String)
