package io.chronos

import java.time.{Clock, Instant, ZoneId, ZonedDateTime}
import java.util.UUID

import io.chronos.id.ExecutionId
import org.scalatest.matchers.{BeMatcher, MatchResult}
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

/**
 * Created by domingueza on 15/07/15.
 */
class ExecutionTest extends FlatSpec with GivenWhenThen with Matchers {
  import Execution._

  val FixedInstant = Instant.ofEpochMilli(893293345)
  val UTCZone      = ZoneId.of("UTC")

  implicit val clock = Clock.fixed(FixedInstant, UTCZone)

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

    And("scheduled as of now")
    assert(execution.stage match {
      case Scheduled(when) =>
        when == ZonedDateTime.now(clock)
      case _ => false
    })
  }

  "An execution" should "not accept an early stage" in {
    Given("An execution ID")
    val executionId: ExecutionId = ((UUID.randomUUID(), 0), 0)

    And("an execution in progress")
    val execution = Execution(executionId) << Started(ZonedDateTime.now(clock), UUID.randomUUID().toString)

    When("Making the execution 'triggered'")
    val thrown = intercept[Exception] {
      execution << Triggered(ZonedDateTime.now(clock))
    }

    Then("the exception is thrown")
    assert(thrown.getMessage === "Can't move the execution to an earlier stage.")
  }

}
