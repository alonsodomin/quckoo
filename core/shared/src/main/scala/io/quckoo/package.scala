package io

import java.util.concurrent.Callable

import io.quckoo.fault.Fault

import scalaz._

/**
 * Created by aalonsodominguez on 07/07/15.
 */
package object quckoo {

  type JobClass = Class[_ <: Callable[_]]

  type Validated[+A] = ValidationNel[Fault, A]

}
