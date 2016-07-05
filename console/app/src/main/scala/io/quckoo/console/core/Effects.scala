package io.quckoo.console.core

import diode._

import scala.concurrent.ExecutionContext
import scalaz.NonEmptyList

/**
  * Created by alonsodomin on 05/07/2016.
  */
private[core] object Effects {

  def seq[E <: Effect](effects: NonEmptyList[E])(implicit ec: ExecutionContext): EffectSeq =
    seq(effects.head, effects.tail.toList: _*)

  def seq(head: Effect, tail: Effect*)(implicit ec: ExecutionContext): EffectSeq =
    new EffectSeq(head, tail, ec)

  def set(head: Effect, tail: Effect*)(implicit ec: ExecutionContext): EffectSet =
    new EffectSet(head, tail.toSet, ec)

}
