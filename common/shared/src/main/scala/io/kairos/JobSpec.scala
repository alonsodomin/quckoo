package io.kairos

import io.kairos.id._
import monocle.macros.Lenses

/**
 * Created by aalonsodominguez on 10/07/15.
 */
@Lenses case class JobSpec(displayName: String,
    description: String = "",
    artifactId: ArtifactId,
    jobClass: String
)
