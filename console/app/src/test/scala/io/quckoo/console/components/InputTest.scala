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

    ComponentTester(Input.component[String])(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.string.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input.component[Password])(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.password.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input.component[Int])(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.int.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input.component[Long])(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.long.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input.component[Date])(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.date.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(Input.component[Time])(Input.Props(None, None, onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.time.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }
  }

}
