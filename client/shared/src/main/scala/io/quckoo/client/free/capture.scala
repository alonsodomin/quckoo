package io.quckoo.client.free

import monix.eval.Task

/**
  * Created by domingueza on 03/11/2016.
  */
object capture {

  trait Capture[M[_]] {
    def apply[A](a: => A): M[A]
  }

  object Capture {
    def apply[M[_]](implicit M: Capture[M]): Capture[M] = M

    implicit val TaskCapture: Capture[Task] = new Capture[Task] {
      def apply[A](a: => A): Task[A] =
        Task.delay(a)
    }
  }

}
