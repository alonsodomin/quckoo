package io.kairos

import io.kairos.id.ArtifactId
import org.scalatest.{FlatSpec, Matchers}

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class JobSpecTest extends FlatSpec with Matchers {
  import Scalaz._

  "Validation for JobSpec parameters" should "not accept nulls" in {
    val expectedErrors = NonEmptyList(
      Required("displayName"),
      NotNull("description"),
      NotNull("artifactId"),
      Required("jobClass")
    ).failure[JobSpec]
    JobSpec.validate(null, null, null, null) should be (expectedErrors)
  }

  it should "not accept empty strings in displayName or jobClass" in {
    val expectedErrors = NonEmptyList(
      Required("displayName"),
      NotNull("artifactId"),
      Required("jobClass")
    ).failure[JobSpec]
    JobSpec.validate("", None, null, "") should be (expectedErrors)
  }

  it should "not accept an invalid artifactId" in {
    val expectedErrors = NonEmptyList(
      Required("groupId"),
      Required("artifactId"),
      Required("version")
    ).failure[JobSpec]
    JobSpec.validate("foo", None, ArtifactId(null, null, null), "bar") should be (expectedErrors)
  }

}
