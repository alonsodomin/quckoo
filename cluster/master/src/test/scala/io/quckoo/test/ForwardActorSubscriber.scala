/*
 * Copyright 2016 Antonio Alonso Dominguez
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.quckoo.test

import akka.actor.{ActorRef, Props}
import akka.stream.actor.{ActorSubscriber, OneByOneRequestStrategy, RequestStrategy}

/**
  * Created by domingueza on 12/07/2016.
  */
object ForwardActorSubscriber {

  def props(ref: ActorRef): Props = Props(classOf[ForwardActorSubscriber], ref)

}

class ForwardActorSubscriber(ref: ActorRef) extends ActorSubscriber {

  override protected def requestStrategy: RequestStrategy = OneByOneRequestStrategy

  override def receive: Receive = {
    case msg => ref forward msg
  }

}
