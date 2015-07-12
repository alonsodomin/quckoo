package components

import javax.inject.{Inject, Singleton}

import actors.ExecutionActor
import akka.actor.{ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import io.chronos.protocol.SchedulerProtocol
import play.api.libs.iteratee.Enumerator

import scala.concurrent.Future
import scala.concurrent.duration._

/**
 * Created by aalonsodominguez on 12/07/15.
 */
@Singleton
class ChronosListener @Inject() (private val system: ActorSystem) {
  import ExecutionActor._
  import SchedulerProtocol._

  private val executionActor = system.actorOf(Props[ExecutionActor])
  
  def executions(subscriptionId: Long): Future[Enumerator[ExecutionEvent]] = {
    implicit val timeout = Timeout(10.seconds)
    
    (executionActor ? OpenChannel(subscriptionId)).asInstanceOf[Future[Enumerator[ExecutionEvent]]]
  }

  def unsubscribe(subscriptionId: Int): Unit = {

  }

}
