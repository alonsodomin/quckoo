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
import com.typesafe.config.{Config, ConfigFactory}
import io.quckoo.cluster.{QuckooClusterSettings, QuckooFacade}
import io.quckoo.time.JDK8TimeSource
import org.slf4s.Logging
import scopt.OptionParser

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

/**
 * Created by domingueza on 09/07/15.
 */
object Boot extends App with Logging {

  val parser = new OptionParser[Options]("quckoo-master") {
    head("quckoo-master", "0.1.0")

    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = Some(b))
    } text "Bind to this external host and port. Useful when using inside Docker containers"

    opt[Int]('p', "port") valueName "port" action { (p, options) =>
      options.copy(port = p)
    } text "Port to use to listen to connections"

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

  def loadConfig(opts: Options): Config =
    opts.toConfig.withFallback(ConfigFactory.load())

  def start(config: Config): Unit = {
    implicit val system = ActorSystem(Options.SystemName, config)
    sys.addShutdownHook {
      log.info("Received kill signal, terminating...")
      system.terminate()
    }

    implicit val timeSource = JDK8TimeSource.default
    val settings = QuckooClusterSettings(system)

    import system.dispatcher
    QuckooFacade.start(settings) onComplete {
      case Success(_) =>
        log.info("Quckoo server initialized!")

      case Failure(ex) =>
        ex.printStackTrace()
        system.terminate()
    }
  }

  parser.parse(args, Options()).foreach { opts =>
    start(loadConfig(opts))
  }

}
