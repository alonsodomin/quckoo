package io.chronos.scheduler2

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.chronos.protocol.{GetJob, ScheduleJob2}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
class FrontEnd(jobRegistry: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd: ScheduleJob2 =>
      val schedule = context.actorOf(Schedule.props(cmd.params, cmd.trigger, cmd.timeout))
      jobRegistry.tell(GetJob(cmd.jobId), schedule)
  }

}
