package io.quckoo.client.http

import io.quckoo.client.core.ProtocolSpecs

/**
  * Created by alonsodomin on 19/09/2016.
  */
sealed trait Http extends HttpProtocol
  with ProtocolSpecs[HttpProtocol]
  with HttpClusterCmds
  with HttpSchedulerCmds
  with HttpRegistryCmds
  with HttpSecurityCmds
  with SSEChannels

object Http extends Http
