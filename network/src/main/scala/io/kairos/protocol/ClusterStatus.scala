package io.kairos.protocol

import akka.cluster.UniqueAddress

/**
 * Created by aalonsodominguez on 24/08/15.
 */
case class ClusterStatus(healthyMembers: Set[UniqueAddress], unreachableMembers: Set[UniqueAddress]) extends ClientEvent
