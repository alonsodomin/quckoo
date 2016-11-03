package io.quckoo.cluster.config

import io.quckoo.resolver.ivy.IvyConfig

/**
  * Created by domingueza on 03/11/2016.
  */
case class ClusterConfig(resolver: IvyConfig, taskQueue: TaskQueueConfig, http: HttpConfig)
