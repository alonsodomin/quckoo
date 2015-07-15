package common

/**
 * Created by aalonsodominguez on 14/07/15.
 */
object MessageTypes {

  sealed trait MessageType

  case object Request extends MessageType
  case object Event extends MessageType

}
