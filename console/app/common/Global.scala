package common

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
import play.api.GlobalSettings

/**
 * Created by domingueza on 09/07/15.
 */
object Global extends GlobalSettings {

  val chronosSystem = ActorSystem("ClusterSystem", ConfigFactory.load("chronos"))

}
