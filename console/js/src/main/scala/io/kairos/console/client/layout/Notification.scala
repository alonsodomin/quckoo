package io.kairos.console.client.layout

import io.kairos.console.client.layout.Notification.Level.Level

/**
 * Created by alonsodomin on 15/10/2015.
 */
object Notification {

  object Level extends Enumeration {
    type Level = Value
    val Error, Warning, Info, Success = Value
  }

}

case class Notification(level: Level, content: String)