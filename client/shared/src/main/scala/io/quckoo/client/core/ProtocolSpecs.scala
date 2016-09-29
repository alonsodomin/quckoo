package io.quckoo.client.core

/**
  * Created by alonsodomin on 19/09/2016.
  */
trait ProtocolSpecs[P <: Protocol]
  extends ClusterCmds[P]
    with SchedulerCmds[P]
    with RegistryCmds[P]
    with SecurityCmds[P]
    with Channels[P]
