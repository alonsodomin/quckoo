package components

import javax.inject.{Inject, Singleton}

import akka.actor.ActorRef

/**
 * Created by domingueza on 09/07/15.
 */
@Singleton
class RemoteScheduler @Inject() (val chronosFacade: ActorRef) {

}
