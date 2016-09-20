package io.quckoo.client.core

import io.quckoo.util._

import monix.reactive.Observable

import scalaz.Kleisli

/**
  * Created by alonsodomin on 17/09/2016.
  */
private[core] final class TestDriverBackend[P <: Protocol](
    stream: Iterable[P#EventType],
    command: P#Request => LawfulTry[P#Response]
  ) extends DriverBackend[P] {

  @inline def send =
    Kleisli[LawfulTry, P#Request, P#Response](command).transform(lawfulTry2Future)

  @inline def open[Ch <: Channel[P]](channel: Ch) =
    Kleisli[Observable, Unit, P#EventType](_ => Observable.fromIterable(stream))

}
