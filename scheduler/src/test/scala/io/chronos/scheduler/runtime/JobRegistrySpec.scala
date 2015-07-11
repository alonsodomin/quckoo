package io.chronos.scheduler.runtime

import java.time.Clock

import com.hazelcast.core.Hazelcast
import io.chronos.scheduler.HazelcastJobRegistry
import io.chronos.test.DummyJob
import org.scalatest.{BeforeAndAfter, FlatSpec, Matchers}

/**
 * Created by domingueza on 10/07/15.
 */
class JobRegistrySpec extends FlatSpec with Matchers with BeforeAndAfter {

  val clock = Clock.systemUTC()
  val hazelcastInstance = Hazelcast.newHazelcastInstance()

  val jobRegistry = new HazelcastJobRegistry(clock, hazelcastInstance)

  "A Job Registry" should "accept publishing Job Specs" in {
    val jobSpec = JobSpec(id = "foo", displayName = "Foo Job", jobClass = classOf[DummyJob])
    jobRegistry.publishSpec(jobSpec)
    assert(jobRegistry(jobSpec.id).get == jobSpec)
  }

}
