package io.kairos.id

import io.kairos.{Fault, Required}
import org.scalatest.{FlatSpec, Matchers}

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class ArtifactIdTest extends FlatSpec with Matchers {
  import Scalaz._

  "Validation for ArtifactId parameters" should "not accept nulls" in {
    val expectedErrors = NonEmptyList(
        Required("groupId"),
        Required("artifactId"),
        Required("version")
    ).failure[ArtifactId]
    ArtifactId.validate(null, null, null) should be (expectedErrors)
  }

  it should "not accept empty strings" in {
    val expectedErrors = NonEmptyList(
      Required("groupId"),
      Required("artifactId"),
      Required("version")
    ).failure[ArtifactId]
    ArtifactId.validate("", "", "") should be (expectedErrors)
  }

  it should "not accept any combination of nulls or empty strings" in {
    val expectedErrors = NonEmptyList(
      Required("groupId"),
      Required("version")
    ).failure[ArtifactId]
    ArtifactId.validate(null, "foo", "") should be (expectedErrors)
  }

  it should "do something" in {
    val artifact = ArtifactId("", "", "")
    println(ArtifactId.validation(artifact))
  }

  it should "accept any other values" in {
    val expectedArtifactId = ArtifactId("foo", "bar", "baz")
    ArtifactId.validate(expectedArtifactId) should be (expectedArtifactId.successNel[Fault])
  }

}
