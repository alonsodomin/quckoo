package io.chronos.scheduler

import akka.actor.{Actor, ActorLogging, ActorRef}
import io.chronos.protocol.{GetJob, ScheduleJob}

/**
 * Created by aalonsodominguez on 16/08/15.
 */
class FrontEnd(jobRegistry: ActorRef) extends Actor with ActorLogging {

  override def receive: Receive = {
    case cmd: ScheduleJob =>
      val schedule = context.actorOf(Schedule.props(cmd.params, cmd.trigger, cmd.timeout))
      jobRegistry.tell(GetJob(cmd.jobId), schedule)
  }

}
