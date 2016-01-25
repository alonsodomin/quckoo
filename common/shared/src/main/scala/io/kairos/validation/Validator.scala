package io.kairos.validation

import io.kairos._

import scalaz._

/**
  * Created by alonsodomin on 24/01/2016.
  */
trait Validator[A] {
  def eval(item: A): Validated[A]
}
object Validator {
  import Scalaz._

  def apply[A](f: A => Validated[A]): Validator[A] = new Validator[A] {
    def eval(item: A): Validated[A] = f(item)
  }

  implicit val instance = new Applicative[Validator] {
    def point[A](a: => A): Validator[A] = Validator { _ => a.successNel[ValidationFault] }

    def map2[A, B, C](fa: Validator[A], fb: Validator[B])(f: (A, B) => C): Validator[C] =
      ap(map(fa)(f.curried))(fb)

    def ap[A, B](fa: => Validator[A])(f: => Validator[A => B]): Validator[B] =
      map2(fa, f)((a, f) => f(a))
  }

  sealed trait Rule[A]
  type Rules[A] = FreeAp[Rule, A]

  trait AnyRefRules {
    case class NotNullRule[S, A](name: String, g: S => A) extends Rule[A]
    case class ValidRule[S, A](name: String, g: S => A, value: Rules[A]) extends Rule[A]
  }
  trait StringRules {
    case class NotEmptyRule[S, A](name: String, g: S => String, value: String => A) extends Rule[A]
  }

  object rules extends AnyRefRules with StringRules

  object anyRefs {
    def notNull[S, A](name: String, item: S)(g: S => A): Validated[A] = {
      val value = g(item)
      if (value == null) NotNull(name).failureNel[A]
      else value.successNel[ValidationFault]
    }

    def valid[S, A](name: String, item: S, ruleSet: Rules[A])(g: S => A): Validated[A] = {
      validate(g(item), name + ".", ruleSet)
    }
  }
  object strings {
    def notEmpty[S](name: String, item: S)(g: S => String): Validated[String] = {
      val value = g(item)
      if (value.isEmpty) Required(name).failureNel[String]
      else value.successNel[ValidationFault]
    }
  }

  object dsl {
    import rules._

    private[this] def getterToFun[S, A](lens: monocle.Lens[S, A]): S => A =
      s => lens.get(s)

    private[this] def lift[A](value: Rule[A]): Rules[A] = FreeAp.lift[Rule, A](value)

    def notNull[A](name: String): Rules[A] = notNull[A, A](name)(identity)
    def notNull[S, A](name: String, lens: monocle.Lens[S, A]): Rules[A] = notNull[S, A](name)(s => lens.get(s))
    def notNull[S, A](name: String)(g: S => A): Rules[A] = lift[A](NotNullRule(name, g))

    def notEmpty(name: String): Rules[String] = notEmpty[String](name)(identity)
    def notEmpty[S](name: String, lens: monocle.Lens[S, String]): Rules[String] = notEmpty[S](name)(getterToFun(lens))
    def notEmpty[S](name: String)(g: S => String): Rules[String] = lift[String](NotEmptyRule(name, g, identity))

    def valid[A](name: String, ruleSet: Rules[A]): Rules[A] = valid[A, A](name, ruleSet)(identity)
    def valid[S, A](name: String, lens: monocle.Lens[S, A], ruleSet: Rules[A]): Rules[A] = valid[S, A](name, ruleSet)(getterToFun(lens))
    def valid[S, A](name: String, ruleSet: Rules[A])(g: S => A): Rules[A] = lift[A](ValidRule(name, g, ruleSet))
  }

  def validate[T](a: T, ruleSet: Rules[T]): Validated[T] =
    validate(a, "", ruleSet)

  private def validate[S](item: S, prefix: String, ruleSet: Rules[S]): Validated[S] = {
    ruleSet.foldMap(new (Rule ~> Validated) {
      import rules._

      def apply[A](rule: Rule[A]): Validated[A] = rule match {
        case NotNullRule(name, g) => anyRefs.notNull(prefix + name, item)(g)
        case NotEmptyRule(name, g, v) => strings.notEmpty[S](prefix + name, item)(g).map(v)
        case ValidRule(name, g, rs) => anyRefs.valid[S, A](prefix + name, item, rs)(g)
      }
    })
  }

  object example {
    import dsl._

    case class Example(a: String, b: String)
    val r: Rules[Example] = (notEmpty[Example]("a")(_.a) |@| notEmpty[Example]("b")(_.b))(Example)

    val valid = validate(Example("", ""), r)
  }

  /*def validate[A](a: A, ruleSet: Rules[A]): Validated[A] = {
    ruleSet.foldMap(new (Rule ~> Validated) {
      import rules._

      override def apply[A](rule: Rule[A]): Validated[A] = rule match {
        case NotNullRule(name, v) =>
          (if (a == null) NotNull(name).failureNel[A]
          else a.successNel[Fault]).map(v)
        case NotEmptyRule(name, v) =>
          (if (a.as[String]))
      }
    })
  }*/

}
