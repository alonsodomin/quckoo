package io.kairos.console.client.components

import scala.collection.mutable

/**
  * Created by alonsodomin on 20/02/2016.
  */
final case class IconState(
    size: Option[Int] = None,
    stack: Option[Int] = None,
    rotate: Option[Int] = None,
    border: Boolean = false,
    fixedWidth: Boolean = false,
    spin: Boolean = false,
    inverse: Boolean = false,
    pulse: Boolean = false,
    flipHorizontal: Boolean = false,
    flipVertical: Boolean = false,
    padding: Boolean = true
)

final case class Icon private[components](name: String, state: IconState = IconState()) {

  def size(s: Int) = copy(state = state.copy(size = Some(s)))
  def stack(s: Int) = copy(state = state.copy(stack = Some(s)))
  def rotate(s: Int) = copy(state = state.copy(rotate = Some(s)))
  def border = copy(state = state.copy(border = true))
  def fixedWidth = copy(state = state.copy(fixedWidth = true))
  def spin = copy(state = state.copy(spin = true))
  def inverse = copy(state = state.copy(inverse = true))
  def pulse = copy(state = state.copy(pulse = true))
  def flipHorizontal = copy(state = state.copy(flipHorizontal = true))
  def flipVertical = copy(state = state.copy(flipVertical = true))
  def noPadding = copy(state = state.copy(padding = false))

  private[components] def classSet = {
    val classSetMap = mutable.Map(
      "fa-border"          -> state.border,
      "fa-fw"              -> state.fixedWidth,
      "fa-spin"            -> state.spin,
      "fa-pulse"           -> state.pulse,
      "fa-inverse"         -> state.inverse,
      "fa-flip-horizontal" -> state.flipHorizontal,
      "fa-flip-vertical"   -> state.flipVertical
    )
    classSetMap += s"fa-$name" -> true
    if (state.size.isDefined) {
      classSetMap += s"fa-${state.size.get}x" -> true
    }
    if (state.stack.isDefined) {
      classSetMap += s"fa-stack-${state.stack.get}x" -> true
    }
    if (state.rotate.isDefined) {
      classSetMap += s"fa-rotate-${state.rotate.get}" -> true
    }
    classSetMap.toMap
  }

}
