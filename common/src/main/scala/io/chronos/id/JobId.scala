package io.chronos.id

import java.security.MessageDigest

import io.chronos.JobSpec

/**
 * Created by aalonsodominguez on 24/08/15.
 */
object JobId {

  def apply(jobSpec: JobSpec): JobId = {
    val md = MessageDigest.getInstance("MD5")
    md.update(jobSpec.moduleId.toString.getBytes("UTF-8"))
    new JobId(new String(md.digest(), "UTF-8"))
  }

  def unapply(hash: String): Option[JobId] =
    if (hash.matches("[a-fA-F0-9]{32}")) Some(new JobId(hash))
    else None

}

final class JobId private (private val hash: String) extends Serializable {

  override def equals(other: Any): Boolean = other match {
    case that: JobId => that.hash equals this.hash
    case _           => false
  }

  override def hashCode = 41 * hash.hashCode

  override def toString = hash

}
