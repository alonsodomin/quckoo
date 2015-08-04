package io.chronos.scheduler.store

import java.util.UUID

import com.hazelcast.core.Hazelcast
import io.chronos.JobSpec
import io.chronos.id.{JobId, ModuleId}
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
 * Created by domingueza on 04/08/15.
 */
object HazelcastRegistryCacheTest extends HazelcastRegistryCache {

  val TestModuleId = ModuleId("io.chronos", "test", "latest")
  val TestJobClass = "com.example.Job"

  val TotalJobs = 30

  val hazelcastInstance = Hazelcast.newHazelcastInstance()

}

class HazelcastRegistryCacheTest extends FlatSpec with BeforeAndAfter with Matchers {

  import HazelcastRegistryCacheTest._

  var jobIds: List[JobId] = Nil

  before {
    // Generate a lot of Job Specs and populate the registry
    for (count <- 1 to TotalJobs) {
      val jobId = UUID.randomUUID()
      HazelcastRegistryCacheTest.registerJob(
        JobSpec(jobId, s"job-$count", "", TestModuleId, TestJobClass)
      )
      jobIds = jobId :: jobIds
    }
  }

  "A HazelcastRegistry" should "stream registered jobs on demand" in {
    val amountToTake = 15
    HazelcastRegistryCacheTest.getJobs.take(amountToTake).count(_ => true) should be (amountToTake)
  }

}
