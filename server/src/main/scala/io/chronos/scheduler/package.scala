package io.chronos

/**
 * Created by aalonsodominguez on 07/07/15.
 */
package object scheduler {

  type JobSpec = Class[_ <: Job]

}
