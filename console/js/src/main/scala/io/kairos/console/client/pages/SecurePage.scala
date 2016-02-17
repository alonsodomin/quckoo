package io.kairos.console.client.pages

import io.kairos.console.client.boot.Routees
import io.kairos.console.client.security.ClientAuth

/**
  * Created by alonsodomin on 17/02/2016.
  */
trait SecurePage extends ClientAuth { self: KairosPage =>
  import KairosPage._

  whenReady { _ =>
    if (!isAuthenticated) {
      Routees.login().go()
      ReadyResult.ShortCircuit
    } else {
      ReadyResult.Continue
    }
  }

}
