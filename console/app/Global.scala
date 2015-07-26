import akka.actor.{AddressFromURIString, RootActorPath}
import akka.contrib.pattern.ClusterClient
import akka.japi.Util._
import com.typesafe.config.ConfigFactory
import play.api.{Application, GlobalSettings}

/**
 * Created by domingueza on 09/07/15.
 */
object Global extends GlobalSettings {

  val ChronosClient = "chronosClient"

  override def onStart(app: Application) = {
    val chronosConf = ConfigFactory.load("chronos")

    val initialContacts = immutableSeq(chronosConf.getStringList("chronos.seed-nodes")).map {
      case AddressFromURIString(addr) => app.actorSystem.actorSelection(RootActorPath(addr) / "user" / "receptionist")
    }.toSet

    app.actorSystem.actorOf(ClusterClient.props(initialContacts), ChronosClient)
  }

}
