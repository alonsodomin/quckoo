package io.chronos

import scala.language.experimental.macros
import scala.reflect.macros.blackbox

/**
 * Created by aalonsodominguez on 08/07/15.
 */
trait Parameterizable

object Parameterizable {
  implicit class Mappable[P <: Parameterizable](val jobDef: P) extends AnyVal {
    def params: Map[String, Any] = macro Macros.params_impl[P]
  }

  private object Macros {

    def params_impl[T: c.WeakTypeTag](c: blackbox.Context) = {
      import c.universe._

      val mapApply = Select(reify(Map).tree, TermName("apply"))
      val jobSchedule = Select(c.prefix.tree, TermName("jobSchedule"))

      val pairs = weakTypeOf[T].decls.collect {
        case m: MethodSymbol if m.isCaseAccessor =>
          val name = c.literal(m.name.decodedName.toString)
          val value = c.Expr(Select(jobSchedule, m.name))
          reify(name.splice -> value.splice).tree
      }

      c.Expr[Map[String, Any]](Apply(mapApply, pairs.toList))
    }

  }
}
