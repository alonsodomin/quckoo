package io.quckoo.cluster.config

import scala.concurrent.duration.FiniteDuration

/**
  * Created by domingueza on 03/11/2016.
  */
case class TaskQueueConfig(maxWorkTimeout: FiniteDuration)