package io

import java.util.concurrent.Callable

import scalaz._

/**
 * Created by aalonsodominguez on 07/07/15.
 */
package object kairos {

  type JobClass = Class[_ <: Callable[_]]

  type Faulty[+A] = Fault \/ A
  type Faults = NonEmptyList[Fault]
  type Validated[+A] = ValidationNel[Fault, A]

}
