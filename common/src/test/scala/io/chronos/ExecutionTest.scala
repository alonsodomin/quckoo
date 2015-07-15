package io.chronos

import java.time.Clock
import java.util.UUID

import io.chronos.id.ExecutionId
import org.scalatest.matchers.{BeMatcher, MatchResult}
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

/**
 * Created by domingueza on 15/07/15.
 */
class ExecutionTest extends FlatSpec with GivenWhenThen with Matchers {
  import Execution._

  implicit val clock = Clock.systemUTC()

  class StatusMatcher(statusType: StatusType) extends BeMatcher[Execution] {
    def apply(left: Execution) = MatchResult(
      left is statusType,
      s"$left was not in $statusType",
      s"$left is in $statusType"
    )
  }

  def status(statusType: StatusType): StatusMatcher = new StatusMatcher(statusType)

  "A new execution instance" should "be in scheduled stage" in {
    Given("An execution ID")
    val executionId: ExecutionId = ((UUID.randomUUID(), 0), 0)

    When("Creating a new execution")
    val execution = Execution(executionId)

    Then("it is pending")
    execution shouldBe status(Pending)
  }

}
