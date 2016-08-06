package io.quckoo.console.components

import io.quckoo.time.{Date, Time}

import japgolly.scalajs.react._
import japgolly.scalajs.react.test._

import org.scalatest.{FlatSpec, Matchers}


/**
  * Created by alonsodomin on 13/04/2016.
  */
class InputTest extends FlatSpec with Matchers {

  "An Input component" should "render an <input> properly typed" in {
    def onUpdate[A]: Input.OnUpdate[A] = _ => Callback.empty

    ComponentTester(Input[String](onUpdate).component)(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.string.html}" value="" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input[Password](onUpdate).component)(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.password.html}" value="" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input[Int](onUpdate).component)(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.int.html}" value="" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input[Long](onUpdate).component)(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.long.html}" value="" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input[Date](onUpdate).component)(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.date.html}" value="" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input[Time](onUpdate).component)(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.time.html}" value="" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }
  }

}
