package io.chronos.resolver

import java.net.URL

import io.chronos.id.JobModuleId

/**
 * Created by aalonsodominguez on 17/07/15.
 */
case class JobModulePackage(moduleId: JobModuleId, classpath: Seq[URL]) {

}
