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

package io.quckoo.worker.boot

import akka.actor.ActorSystem
import akka.cluster.client.{ClusterClient, ClusterClientSettings}

import com.typesafe.config.{Config, ConfigFactory}

import io.quckoo.{Info, Logo}
import io.quckoo.config._
import io.quckoo.resolver.Resolver
import io.quckoo.resolver.ivy.IvyResolve
import io.quckoo.worker.config.{ResolverDispatcher, WorkerSettings}
import io.quckoo.worker.core.Worker
import io.quckoo.worker.SystemName
import io.quckoo.worker.executor.DefaultTaskExecutorProvider

import slogging._

import scopt.OptionParser

/**
  * Created by domingueza on 09/07/15.
  */
object Boot extends App with LazyLogging {
  LoggerConfig.factory = SLF4JLoggerFactory()
  LoggerConfig.level = LogLevel.DEBUG

  val parser = new OptionParser[CliOptions]("quckoo-worker") {
    head("quckoo-worker", Info.version)
    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = Some(b))
    } text "Bind to this external host and port. Useful when using inside Docker containers"

    opt[Int]('p', "port") valueName "<port>" action { (p, options) =>
      options.copy(port = p)
    } text "Worker node port"

    opt[Seq[String]]("master") valueName "<host:port>,<host:port>" action {
      (nodes, options) =>
        options.copy(masterNodes = nodes)
    } text "Comma separated list of Quckoo master nodes"
  }

  def loadConfig(opts: CliOptions): Config =
    opts.toConfig.withFallback(ConfigFactory.load())

  def start(config: Config): Unit = {
    logger.info(s"Starting Quckoo Worker ${Info.version}...\n" + Logo)

    implicit val system = ActorSystem(SystemName, config)
    sys.addShutdownHook { system.terminate() }

    WorkerSettings(config) match {
      case Right(settings) => doStart(settings)
      case Left(errors)    =>
        logger.error("Error reading configuration. \n{}",
          describeConfigFailures(errors).mkString("\n")
        )
    }
  }

  private def doStart(settings: WorkerSettings)(implicit system: ActorSystem): Unit = {
    val clientSettings = {
      val ccs = ClusterClientSettings(system)
      if (settings.worker.contactPoints.nonEmpty)
        ccs.withInitialContacts(settings.worker.contactPoints.map(_.actorPath))
      else ccs
    }
    val clusterClient  = system.actorOf(ClusterClient.props(clientSettings), "client")

    val ivyResolve = IvyResolve(settings.resolver)
    settings.resolver.createFolders()

    // TODO resolver should be re-implemented since there is not need to be an actor
    val resolverProps    = Resolver.props(ivyResolve).withDispatcher(ResolverDispatcher)

    system.actorOf(Worker.props(clusterClient, resolverProps, DefaultTaskExecutorProvider), "worker")
  }

  parser.parse(args, CliOptions()).foreach { opts =>
    start(loadConfig(opts))
  }

}
