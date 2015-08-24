package io.chronos.protocol

import akka.cluster.Member

/**
 * Created by aalonsodominguez on 24/08/15.
 */
case class ClusterStatus(healthyMembers: Set[Member], unreachableMembers: Set[Member])
