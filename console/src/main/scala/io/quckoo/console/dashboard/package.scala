package io.quckoo.console

import io.quckoo.console.core.ConsoleCircuit.Implicits.consoleClock
import io.quckoo.console.log._
import io.quckoo.protocol.Event
import io.quckoo.protocol.cluster.MasterRemoved

/**
  * Created by alonsodomin on 07/05/2017.
  */
package object dashboard {

  final val DashboardLogger: Logger[Event] = {
    case MasterRemoved(nodeId) =>
      LogRecord.warning(s"Master node $nodeId has left the cluster.")
  }

}
