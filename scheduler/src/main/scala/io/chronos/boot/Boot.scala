package io.chronos.boot

import java.time.Clock

import akka.actor._
import com.typesafe.config.ConfigFactory
import io.chronos.cluster.{ChronosCluster, ChronosClusterSettings}

/**
 * Created by domingueza on 09/07/15.
 */
object Boot {

  val DefaultPort = 2551

  def main(args: Array[String]): Unit = {
    val port = if (args.length > 0) args(0).toInt else DefaultPort

    val defaultConf = ConfigFactory.load("reference.conf")
    val conf = ConfigFactory.parseString("akka.remote.netty.tcp.port=" + port).
      withFallback(ConfigFactory.load()).
      withFallback(defaultConf)

    val system = ActorSystem("ChronosClusterSystem", conf)
    implicit val clock = Clock.systemUTC()

    val settings = ChronosClusterSettings(system)
    val chronosProps  = ChronosCluster.props(settings)
    system.actorOf(chronosProps, "chronos")
  }

}
