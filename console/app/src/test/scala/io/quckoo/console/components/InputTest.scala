package io.quckoo.console.components

import japgolly.scalajs.react._
import japgolly.scalajs.react.test._
import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alonsodomin on 13/04/2016.
  */
class InputTest extends FlatSpec with Matchers {

  "An Input component for an Int" should "render an <input> typed as number" in {
    val input = new Input[Int](_ => Callback.empty)

    val expectedHtml: String = s"""<input type="${Input.Type.int.html}" class="form-control">"""

    ComponentTester(input.component)(Input.Props(None, None, input.onUpdate, Seq.empty)) { tester =>
      import tester._

      def assertHtml() = {
        assert(component.outerHtmlWithoutReactDataAttr() == expectedHtml)
      }

      assertHtml()
    }
  }

}
