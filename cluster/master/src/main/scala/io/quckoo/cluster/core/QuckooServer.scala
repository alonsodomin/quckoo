package io.quckoo.cluster.core

import io.quckoo.api.{Cluster, Registry, Scheduler}
import io.quckoo.cluster.registry.RegistryStreams
import io.quckoo.cluster.scheduler.SchedulerStreams

/**
 * Created by alonsodomin on 14/10/2015.
 */
trait QuckooServer extends Auth
    with Cluster
    with Registry with RegistryStreams
    with Scheduler with SchedulerStreams
