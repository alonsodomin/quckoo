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

package io.quckoo.cluster

/**
  * Created by alonsodomin on 04/11/2016.
  */
package object config {

  final val AkkaArteryCanonicalHost = "akka.remote.artery.canonical.hostname"
  final val AkkaArteryCanonicalPort = "akka.remote.artery.canonical.port"
  final val AkkaArteryBindHost = "akka.remote.artery.bind.hostname"
  final val AkkaArteryBindPort = "akka.remote.artery.bind.port"

  final val AkkaClusterSeedNodes = "akka.cluster.seed-nodes"

  final val CassandraJournalContactPoints  = "cassandra-journal.contact-points"
  final val CassandraSnapshotContactPoints = "cassandra-snapshot-store.contact-points"

  final val QuckooHttpBindInterface = "quckoo.http.bind-interface"
  final val QuckooHttpBindPort      = "quckoo.http.bind-port"

  final val HostAndPort = """(.+?):(\d+)""".r

  final val DefaultHttpInterface = "0.0.0.0"
  final val DefaultHttpPort      = 8095

  final val DefaultUdpInterface = "127.0.0.1"
  final val DefaultUdpPort      = 25520

}
