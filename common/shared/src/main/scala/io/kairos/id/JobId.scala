package io.kairos.id

import java.util.UUID

import io.kairos.JobSpec

/**
 * Created by aalonsodominguez on 24/08/15.
 */
object JobId {

  def apply(jobSpec: JobSpec): JobId = {
    val plainId = s"${jobSpec.artifactId.toString}!${jobSpec.jobClass}"
    val id = UUID.nameUUIDFromBytes(plainId.getBytes("UTF-8"))
    new JobId(id.toString)
  }

  @inline def apply(id: UUID): JobId = new JobId(id.toString)

}

final case class JobId(private val id: String) {

  override def toString = id

}
