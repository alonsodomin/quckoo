package io.chronos

import java.time.Clock
import java.util.UUID

import io.chronos.id.ExecutionId
import org.scalatest.{FlatSpec, GivenWhenThen, Matchers}

/**
 * Created by domingueza on 15/07/15.
 */
class ExecutionTest extends FlatSpec with GivenWhenThen with Matchers {
  import Execution._

  implicit val clock = Clock.systemUTC()

  "A new execution instance" should "be in scheduled stage" in {
    Given("An execution ID")
    val executionId: ExecutionId = ((UUID.randomUUID(), 0), 0)

    When("Creating a new execution")
    val execution = Execution(executionId)

    Then("it is on scheduled stage")
    assert(execution is ScheduledStage)
  }

}
