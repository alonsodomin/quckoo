package io.kairos.id

import java.util.UUID

import io.kairos.JobSpec

/**
 * Created by aalonsodominguez on 24/08/15.
 */
object JobId {
  import scala.language.implicitConversions

  def apply(jobSpec: JobSpec): JobId = {
    val plainId = s"${jobSpec.artifactId.toString}!${jobSpec.jobClass}"
    val id = UUID.nameUUIDFromBytes(plainId.getBytes("UTF-8"))
    new JobId(id.toString)
  }

  @inline
  def apply(id: UUID): JobId = new JobId(id.toString)

  implicit def jobIdToString(jobId: JobId): String = jobId.toString

}

final case class JobId(private val id: String) extends Serializable {

  override def equals(other: Any): Boolean = other match {
    case that: JobId => that.id equals this.id
    case _           => false
  }

  override def hashCode = id.hashCode

  override def toString = id

}
