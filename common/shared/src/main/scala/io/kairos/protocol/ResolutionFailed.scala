package io.kairos.protocol

/**
  * Created by alonsodomin on 18/12/2015.
  */
sealed trait ResolutionFailed {
  def description: String
}

case class UnresolvedDependencies(value: Seq[String]) extends ResolutionFailed {

  def description = s"Unresolved dependencies: ${value.mkString(",")}"

}
