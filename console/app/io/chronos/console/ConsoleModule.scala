package io.chronos.console

import akka.actor.ActorRef
import com.google.inject.AbstractModule
import com.google.inject.name.Names
import play.libs.Akka

/**
 * Created by domingueza on 09/07/15.
 */
class ConsoleModule extends AbstractModule {

  override def configure(): Unit = {
    val chronosFacade = Akka.system().actorOf(ChronosFacade.props, "chronos")

    bind(classOf[ActorRef]).
      annotatedWith(Names.named("chronos")).
      toInstance(chronosFacade)
  }

}
