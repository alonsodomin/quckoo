package io.chronos.scheduler.runtime

import java.time.Clock
import java.util.UUID

import com.hazelcast.core.Hazelcast
import io.chronos.JobSpec
import io.chronos.id.JobModuleId
import io.chronos.scheduler.HazelcastJobRegistry
import io.chronos.test.DummyJob
import org.scalatest._

/**
 * Created by domingueza on 10/07/15.
 */
class JobRegistrySpec extends FlatSpec with Matchers with BeforeAndAfter {

  implicit val clock = Clock.systemUTC()
  val hazelcastInstance = Hazelcast.newHazelcastInstance()

  val jobRegistry = new HazelcastJobRegistry(hazelcastInstance)

  "A Job Registry" should "accept publishing Job Specs" in {
    val jobModuleId = JobModuleId("io.chronos", "examples_2.11", "0.1.0")
    val jobSpec = JobSpec(id = UUID.randomUUID(), displayName = "Foo Job", moduleId = jobModuleId, jobClass = classOf[DummyJob].getName)
    jobRegistry.registerJobSpec(jobSpec)

    assert(jobRegistry.specById(jobSpec.id).get == jobSpec)
  }

}
