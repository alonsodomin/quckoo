package io.quckoo.util

/**
  * Created by alonsodomin on 21/10/2016.
  */
trait IsTraversable[A] {
  type Elem
  type T[x] <: Traversable[_]

  def subst(a: A): T[Elem]
}

object IsTraversable {
  def apply[A](implicit ev: IsTraversable[A]): IsTraversable[A] = ev

  implicit def mk[A, E, T0[_] <: Traversable[_]](implicit ev: A => T0[E]): IsTraversable[A] = new IsTraversable[A] {
    type Elem = E
    type T[x] = T0[x]

    def subst(a: A): T[E] = ev(a)
  }
}
