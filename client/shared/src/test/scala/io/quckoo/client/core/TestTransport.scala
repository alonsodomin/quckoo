package io.quckoo.client.core

import io.quckoo.util._

import scala.concurrent.Future

import scalaz.Kleisli

/**
  * Created by alonsodomin on 17/09/2016.
  */
private[core] final class TestTransport[P <: Protocol](run: P#Request => LawfulTry[P#Response]) extends Transport[P] {

  @inline override def send: Kleisli[Future, P#Request, P#Response] =
    Kleisli[LawfulTry, P#Request, P#Response](run).transform(lawfulTry2Future)

}
