package io.chronos

import akka.actor.{Props, Actor}
import spray.routing.HttpService

/**
 * Created by aalonsodominguez on 05/07/15.
 */
class ApiActor extends HttpService with Actor {

  def actorRefFactory = context

  def receive = {
    case _ =>
  }

}

object ApiActor {
  def props = Props[ApiActor]
}
