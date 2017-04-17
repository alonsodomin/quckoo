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

package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.test._

import org.scalatest.{FlatSpec, Matchers}
import org.threeten.bp.{LocalDate, LocalTime}


/**
  * Created by alonsodomin on 13/04/2016.
  */
class InputTest extends FlatSpec with Matchers {

  "An Input component" should "render an <input> properly typed" in {
    def onUpdate[A]: Input.OnUpdate[A] = _ => Callback.empty

    ReactTestUtils.withRenderedIntoDocument(Input[String].component(Input.Props(None, None, onUpdate, Seq.empty))) { comp =>
      val initialHtml = s"""<input type="${Input.Type.string.html}" value="" class="form-control">"""
      comp.outerHtmlScrubbed() shouldBe initialHtml
    }

    ReactTestUtils.withRenderedIntoDocument(Input[String].component(Input.Props(None, None, onUpdate, Seq.empty))) { comp =>
      val initialHtml = s"""<input type="${Input.Type.string.html}" value="" class="form-control">"""
      comp.outerHtmlScrubbed() shouldBe initialHtml
    }

    ReactTestUtils.withRenderedIntoDocument(Input[Password].component(Input.Props(None, None, onUpdate, Seq.empty))) { comp =>
      val initialHtml = s"""<input type="${Input.Type.password.html}" value="" class="form-control">"""
      comp.outerHtmlScrubbed() shouldBe initialHtml
    }

    ReactTestUtils.withRenderedIntoDocument(Input[Int].component(Input.Props(None, None, onUpdate, Seq.empty))) { comp =>
      val initialHtml = s"""<input type="${Input.Type.int.html}" value="" class="form-control">"""
      comp.outerHtmlScrubbed() shouldBe initialHtml
    }

    ReactTestUtils.withRenderedIntoDocument(Input[Long].component(Input.Props(None, None, onUpdate, Seq.empty))) { comp =>
      val initialHtml = s"""<input type="${Input.Type.long.html}" value="" class="form-control">"""
      comp.outerHtmlScrubbed() shouldBe initialHtml
    }

    ReactTestUtils.withRenderedIntoDocument(Input[LocalDate].component(Input.Props(None, None, onUpdate, Seq.empty))) { comp =>
      val initialHtml = s"""<input type="${Input.Type.date.html}" value="" class="form-control">"""
      comp.outerHtmlScrubbed() shouldBe initialHtml
    }

    ReactTestUtils.withRenderedIntoDocument(Input[LocalTime].component(Input.Props(None, None, onUpdate, Seq.empty))) { comp =>
      val initialHtml = s"""<input type="${Input.Type.time.html}" value="" class="form-control">"""
      comp.outerHtmlScrubbed() shouldBe initialHtml
    }
  }

}
