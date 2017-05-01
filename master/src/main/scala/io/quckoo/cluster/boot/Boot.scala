/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.cluster.boot

import akka.actor._

import com.typesafe.config.ConfigFactory

import io.quckoo._
import io.quckoo.cluster.config.ClusterSettings
import io.quckoo.cluster.{QuckooFacade, SystemName}
import io.quckoo.time.implicits.systemClock

import kamon.Kamon

import slogging._

import scopt.OptionParser

import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by domingueza on 09/07/15.
  */
object Boot extends LazyLogging {

  val parser = new OptionParser[CliOptions]("quckoo-master") {
    head("quckoo-master", Info.version)

    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = Some(b))
    } text "Bind to this external host and port. Useful when using inside Docker containers"

    opt[Int]('p', "port") valueName "port" action { (p, options) =>
      options.copy(port = p)
    } text "Port to use to listen to connections"

    opt[String]("http") valueName "http" action { (value, options) =>
      options.copy(httpBindAddress = Some(value))
    } text "HTTP Address to use to serve the web UI"

    opt[Int]("httpPort") valueName "port" action { (p, options) =>
      options.copy(httpPort = Some(p))
    } text "HTTP port to use to serve the web UI"

    opt[Unit]("seed") action { (_, options) =>
      options.copy(seed = true)
    } text "Flag that indicates that this node will be a seed node. Defaults to true if the list of seed nodes is empty."

    opt[Seq[String]]("nodes") valueName "<host:port>,<host:port>" action { (nodes, options) =>
      options.copy(seedNodes = nodes)
    } text "Comma separated list of Quckoo cluster seed nodes"

    opt[Seq[String]]("cs") valueName "<host:port>,<host:port>" action { (seedNodes, options) =>
      options.copy(cassandraSeedNodes = seedNodes)
    } text "Comma separated list of Cassandra seed nodes (same for Journal and Snapshots)"
    help("help") text "prints this usage text"
  }

  def main(args: Array[String]): Unit = {
    LoggerConfig.factory = SLF4JLoggerFactory()
    LoggerConfig.level = LogLevel.DEBUG

    parser.parse(args, CliOptions()).foreach { opts =>
      logger.info(s"Starting Quckoo Server ${Info.version}...\n" + Logo)
      Kamon.start()

      val config = opts.toConfig.withFallback(ConfigFactory.load())

      implicit val system = ActorSystem(SystemName, config)
      sys.addShutdownHook {
        logger.info("Received kill signal, terminating...")
        Kamon.shutdown()
        Await.ready(system.terminate(), 10 seconds)
      }

      ClusterSettings(config)
        .map(startCluster)
        .recover {
          case ex =>
            logger.error("Could not load configuration.", ex)
        }
    }
  }

  def startCluster(settings: ClusterSettings)(implicit actorSystem: ActorSystem): Unit = {
    import actorSystem.dispatcher

    QuckooFacade.start(settings)
      .map(_ => logger.info("Quckoo server initialized!"))
      .recoverWith { case ex =>
        logger.error("Error initializing Quckoo master node.", ex)
        actorSystem.terminate()
      }
      .foreach(_ => logger.debug("Bootstrap process completed."))
  }

}
