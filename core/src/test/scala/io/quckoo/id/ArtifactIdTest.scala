package io.quckoo.id

import io.quckoo.validation._

import org.scalatest.{FlatSpec, Matchers}

import scalaz._
import Scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
class ArtifactIdTest extends FlatSpec with Matchers {
  import Violation._

  "ArtifactId" should "not accept empty strings" in {
    val expectedErrors = NonEmptyList(
      PathViolation(Path("organization"), Empty),
      PathViolation(Path("name"), Empty),
      PathViolation(Path("version"), Empty)
    ).flatMap(identity).failure[ArtifactId]
    ArtifactId.valid.run(ArtifactId("", "", "")) shouldBe expectedErrors
  }

  it should "accept any other values" in {
    val expectedArtifactId = ArtifactId("foo", "bar", "baz")
    ArtifactId.valid.run(expectedArtifactId) shouldBe expectedArtifactId.successNel[Violation]
  }

}
