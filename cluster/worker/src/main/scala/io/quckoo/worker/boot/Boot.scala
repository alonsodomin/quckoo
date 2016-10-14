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

import akka.actor.{ActorSystem, AddressFromURIString, RootActorPath}
import akka.cluster.client.{ClusterClient, ClusterClientSettings}
import akka.japi.Util._

import com.typesafe.config.{Config, ConfigFactory}

import io.quckoo.Info
import io.quckoo.resolver.Resolver
import io.quckoo.resolver.ivy.{IvyConfiguration, IvyResolve}
import io.quckoo.worker.{JobExecutor, Worker}

import org.slf4s.Logging

import scopt.OptionParser

/**
  * Created by domingueza on 09/07/15.
  */
object Boot extends App with Logging {

  val parser = new OptionParser[Options]("quckoo-worker") {
    head("quckoo-worker", "0.1.0")
    opt[String]('b', "bind") valueName "<host>:<port>" action { (b, options) =>
      options.copy(bindAddress = Some(b))
    } text "Bind to this external host and port. Useful when using inside Docker containers"

    opt[Int]('p', "port") valueName "<port>" action { (p, options) =>
      options.copy(port = p)
    } text "Worker node port"

    opt[Seq[String]]("master") required () valueName "<host:port>,<host:port>" action {
      (nodes, options) =>
        options.copy(masterNodes = nodes)
    } text "Comma separated list of Quckoo master nodes"
  }

  def loadConfig(opts: Options): Config =
    opts.toConfig.withFallback(ConfigFactory.load())

  def start(config: Config): Unit = {
    log.info(s"Starting Quckoo Worker ${Info.version} ...")

    val system = ActorSystem(Options.SystemName, config)
    sys.addShutdownHook { system.terminate() }

    val initialContacts = immutableSeq(config.getStringList(Options.QuckooContactPoints)).map {
      case AddressFromURIString(addr) => RootActorPath(addr) / "system" / "receptionist"
    }.toSet

    val clientSettings = ClusterClientSettings(system).withInitialContacts(initialContacts)
    val clusterClient  = system.actorOf(ClusterClient.props(clientSettings), "client")

    val ivyConfig  = IvyConfiguration(config.getConfig("quckoo"))
    val ivyResolve = IvyResolve(ivyConfig)

    val resolverProps    = Resolver.props(ivyResolve).withDispatcher("quckoo.resolver.dispatcher")
    val jobExecutorProps = JobExecutor.props.withDispatcher("quckoo.worker.dispatcher")

    system.actorOf(Worker.props(clusterClient, resolverProps, jobExecutorProps), "worker")
  }

  parser.parse(args, Options()).foreach { opts =>
    start(loadConfig(opts))
  }

}
