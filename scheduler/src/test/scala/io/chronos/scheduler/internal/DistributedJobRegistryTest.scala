package io.chronos.scheduler.internal

import java.util.UUID

import io.chronos.JobSpec
import io.chronos.id.JobModuleId
import org.apache.ignite.{Ignite, Ignition}
import org.scalatest.{BeforeAndAfter, FlatSpec}

/**
 * Created by aalonsodominguez on 27/07/15.
 */
class DistributedJobRegistryTest extends FlatSpec with BeforeAndAfter {

  class DistributedJobRegistryImpl(val ignite: Ignite) extends DistributedJobRegistry

  var mockIgnite: Ignite = _
  var jobRegistry: DistributedJobRegistry = _

  before {
    mockIgnite = Ignition.ignite()
    jobRegistry = new DistributedJobRegistryImpl(mockIgnite)
  }

  "A distributed job registry" should "persist job specs asynchronously" in {
    val jobSpec = JobSpec(
      id = UUID.randomUUID(),
      displayName = "Foo",
      moduleId = JobModuleId("com.example", "bar", "SNAPSHOT"),
      jobClass = "com.example.Bar"
    )

    jobRegistry.registerJobSpec(jobSpec)
  }

}
