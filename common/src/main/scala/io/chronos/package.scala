package io

import java.util.concurrent.Callable

/**
 * Created by aalonsodominguez on 07/07/15.
 */
package object chronos {

  type JobClass = Class[_ <: Callable[_]]

}
