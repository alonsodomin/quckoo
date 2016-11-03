package io.quckoo.cluster.config

import com.typesafe.config.Config

import io.quckoo.resolver.ivy.IvyConfig

import scala.util.Try

import pureconfig._

/**
  * Created by domingueza on 03/11/2016.
  */
case class ClusterConfig(resolver: IvyConfig, taskQueue: TaskQueueConfig, http: HttpConfig)

object ClusterConfig {
  final val Namespace = "quckoo"

  implicit def clusterFieldMapping[A] = ConfigFieldMapping.apply[A](CamelCase, KebabCase)

  def apply(config: Config): Try[ClusterConfig] =
    loadConfig[ClusterConfig](config, Namespace)

}