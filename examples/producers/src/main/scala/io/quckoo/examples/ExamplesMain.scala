/*
 * Copyright 2015 A. Alonso Dominguez
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

package io.quckoo.examples

import akka.actor.{ActorSystem, AddressFromURIString, Props, RootActorPath}
import akka.cluster.client.ClusterClientSettings
import akka.japi.Util._

import com.typesafe.config.{Config, ConfigFactory}

import io.quckoo.client.tcp.QuckooTcpClient
import io.quckoo.examples.parameters.PowerOfNActor
import io.quckoo.protocol.client._

import scopt.OptionParser

/**
  * Created by aalonsodominguez on 26/07/15.
  */
object ExamplesMain extends App {

  val parser = new OptionParser[CliOptions]("example-producers") {
    head("example-producers", "0.1.0")
    opt[Seq[String]]('c', "cluster") required () valueName "<host>:<port>" action {
      (c, options) =>
        options.copy(clusterNodes = c)
    } text "Comma separated list of Chronos cluster nodes to connect to"
  }

  def loadConfig(opts: CliOptions): Config =
    ConfigFactory.parseMap(opts.asJavaMap).withFallback(ConfigFactory.load())

  def start(config: Config): Unit = {
    val system = ActorSystem("QuckooExamplesSystem", config)

    val initialContacts =
      immutableSeq(config.getStringList(CliOptions.QuckooContactPoints)).map {
        case AddressFromURIString(addr) =>
          RootActorPath(addr) / "system" / "receptionist"
      }.toSet

    val clientSettings =
      ClusterClientSettings(system).withInitialContacts(initialContacts)
    val client = system.actorOf(QuckooTcpClient.props(clientSettings), "client")
    client ! Connect

    system.actorOf(Props(classOf[PowerOfNActor], client), "powerOfN")
  }

  parser.parse(args, CliOptions()).foreach { opts =>
    start(loadConfig(opts))
  }

}
