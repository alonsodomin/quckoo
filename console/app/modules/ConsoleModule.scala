package modules

import com.google.inject.AbstractModule
import com.hazelcast.client.HazelcastClient
import com.hazelcast.core.HazelcastInstance
import io.chronos.internal.HazelcastJobRegistry
import io.chronos.{ExecutionPlan, JobRepository}

/**
 * Created by domingueza on 09/07/15.
 */
class ConsoleModule extends AbstractModule {

  override def configure(): Unit = {
    val hazelcastClient = HazelcastClient.newHazelcastClient()
    bind(classOf[HazelcastInstance]).toInstance(hazelcastClient)

    val hazelcastJobRegistry = new HazelcastJobRegistry(hazelcastClient)
    bind(classOf[JobRepository]).toInstance(hazelcastJobRegistry)
    bind(classOf[ExecutionPlan]).toInstance(hazelcastJobRegistry)
  }

}
