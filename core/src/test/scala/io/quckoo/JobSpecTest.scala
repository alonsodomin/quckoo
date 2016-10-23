package io.quckoo

import io.quckoo.id.ArtifactId
import io.quckoo.validation._

import org.scalatest.{FlatSpec, Matchers}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class JobSpecTest extends FlatSpec with Matchers {
  import Violation._

  "Validation for JobSpec parameters" should "not accept empty values" in {
    val expectedErrors = NonEmptyList(
      PathViolation(Path("displayName"), Empty),
      PathViolation(Path("artifactId", "organization"), Empty),
      PathViolation(Path("artifactId", "name"), Empty),
      PathViolation(Path("artifactId", "version"), Empty),
      PathViolation(Path("jobClass"), Empty)
    ).flatMap(identity).failure[JobSpec]

    JobSpec.valid.run(JobSpec("", None, ArtifactId("", "", ""), "")) shouldBe expectedErrors
  }

}
