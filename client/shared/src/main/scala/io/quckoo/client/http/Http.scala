package io.quckoo.client.http

import io.quckoo.client.core.AllProtocolCmds

/**
  * Created by alonsodomin on 19/09/2016.
  */
sealed trait Http extends HttpProtocol
  with AllProtocolCmds[HttpProtocol]
  with HttpClusterCmds
  with HttpSchedulerCmds
  with HttpRegistryCmds
  with HttpSecurityCmds

object Http extends Http
