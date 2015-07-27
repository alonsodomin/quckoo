package io.chronos.scheduler.internal

import java.util.UUID

import io.chronos.JobSpec
import io.chronos.id.JobModuleId
import org.apache.ignite.{Ignite, Ignition}
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures

/**
 * Created by aalonsodominguez on 27/07/15.
 */
class DistributedJobRegistryTest extends FlatSpec with BeforeAndAfter with Matchers with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  class DistributedJobRegistryImpl(val ignite: Ignite) extends DistributedJobRegistry

  var ignite: Ignite = _
  var jobRegistry: DistributedJobRegistry = _

  val jobSpec = JobSpec(
    id = UUID.randomUUID(),
    displayName = "Foo",
    moduleId = JobModuleId("com.example", "bar", "SNAPSHOT"),
    jobClass = "com.example.Bar"
  )

  before {
    ignite = Ignition.start()
    jobRegistry = new DistributedJobRegistryImpl(ignite)
  }

  "A distributed job registry" should "persist job specs asynchronously" in {
    whenReady(jobRegistry.registerJobSpec(jobSpec)) { result =>
      result should be (jobSpec.id)
    }
  }

  it should "return the previously registered job spec" in {
    whenReady(jobRegistry.specById(jobSpec.id)) {
      case Some(returnedSpec) =>
        returnedSpec should be (jobSpec)
    }
  }

  it should "return the registered job spec when available jobs are retrieved" in {
    whenReady(jobRegistry.availableJobSpecs) { result =>
      result should contain (jobSpec)
    }
  }

}
