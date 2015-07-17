package io.chronos.resolver

import org.apache.ivy.core.resolve.IvyNode

/**
 * Created by aalonsodominguez on 17/07/15.
 */
case class InvalidJobModule(unresolvedDependencies: Seq[IvyNode]) {

}
