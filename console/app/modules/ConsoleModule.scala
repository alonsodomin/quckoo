package modules

import akka.actor.ActorRef
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import common.Global

/**
 * Created by domingueza on 09/07/15.
 */
class ConsoleModule extends AbstractModule {

  override def configure(): Unit = {
    bind(classOf[ActorRef]).
      annotatedWith(Names.named("chronos")).
      toInstance(Global.chronosClient)

  }

}
