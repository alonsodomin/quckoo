package io.quckoo.console.components

import io.quckoo.time.{MomentJSDate, MomentJSTime}
import japgolly.scalajs.react._
import japgolly.scalajs.react.test._
import org.scalatest.{FlatSpec, Matchers}


/**
  * Created by alonsodomin on 13/04/2016.
  */
class InputTest extends FlatSpec with Matchers {

  "An Input component" should "render an <input> properly typed" in {
    def onUpdate[A]: Input.OnUpdate[A] = _ => Callback.empty

    val stringInput = new Input[String](onUpdate)
    val intInput = new Input[Int](onUpdate)
    val longInput = new Input[Long](onUpdate)
    val dateInput = new Input[MomentJSDate](onUpdate)
    val timeInput = new Input[MomentJSTime](onUpdate)

    ComponentTester(stringInput.component)(Input.Props(None, None, stringInput.onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.string.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(intInput.component)(Input.Props(None, None, intInput.onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.int.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(longInput.component)(Input.Props(None, None, longInput.onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.long.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(dateInput.component)(Input.Props(None, None, dateInput.onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.date.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }

    ComponentTester(timeInput.component)(Input.Props(None, None, timeInput.onUpdate, Seq.empty)) { tester =>
      import tester._

      val initialHtml = s"""<input type="${Input.Type.time.html}" class="form-control">"""
      component.outerHtmlWithoutReactDataAttr() should be (initialHtml)
    }
  }

}
