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

package io.quckoo.serialization.kryo

import com.esotericsoftware.kryo.io.{Input, Output}
import com.esotericsoftware.kryo.{Kryo, Serializer}

import io.quckoo.id.{JobId, NodeId, PlanId, TaskId}

/**
  * Created by alonsodomin on 27/02/2017.
  */
object IdSerializers {

  def addDefaultSerliazers(kryo: Kryo): Unit = {
    kryo.addDefaultSerializer(classOf[JobId], JobIdSerializer)
    kryo.addDefaultSerializer(classOf[PlanId], PlanIdSerializer)
    kryo.addDefaultSerializer(classOf[TaskId], TaskIdSerializer)
    kryo.addDefaultSerializer(classOf[NodeId], NodeIdSerializer)
  }

  private object JobIdSerializer extends Serializer[JobId] {
    setImmutable(true)

    override def write(kryo: Kryo, output: Output, `object`: JobId): Unit =
      output.writeString(`object`.toString)

    override def read(kryo: Kryo, input: Input, `type`: Class[JobId]): JobId =
      JobId(input.readString())
  }

  private object PlanIdSerializer extends Serializer[PlanId] {
    setImmutable(true)

    override def write(kryo: Kryo, output: Output, `object`: PlanId): Unit =
      output.writeString(`object`.toString)

    override def read(kryo: Kryo, input: Input, `type`: Class[PlanId]): PlanId =
      PlanId(input.readString())
  }

  private object TaskIdSerializer extends Serializer[TaskId] {
    setImmutable(true)

    override def write(kryo: Kryo, output: Output, `object`: TaskId): Unit =
      output.writeString(`object`.toString)

    override def read(kryo: Kryo, input: Input, `type`: Class[TaskId]): TaskId =
      TaskId(input.readString())

  }

  private object NodeIdSerializer extends Serializer[NodeId] {
    setImmutable(true)

    override def write(kryo: Kryo, output: Output, `object`: NodeId): Unit =
      output.writeString(`object`.toString)

    override def read(kryo: Kryo, input: Input, `type`: Class[NodeId]): NodeId =
      NodeId(input.readString())

  }

}
