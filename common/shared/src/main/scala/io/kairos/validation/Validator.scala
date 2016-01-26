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
  import scala.language.implicitConversions

  def apply[A](f: A => Validated[A]): Validator[A] = new Validator[A] {
    def eval(item: A): Validated[A] = f(item)
  }

  type ValidatorCo[A] = Coyoneda[Validator, A]
  def liftC[A](validator: Validator[A]): ValidatorCo[A] = Coyoneda.lift(validator)

  type ValidatorMonad[A] = Free[ValidatorCo, A]
  def liftM[A](validator: Validator[A]): ValidatorMonad[A] = Free.liftFC(validator)

  sealed trait Rule[A]
  type Rules[A] = FreeAp[Rule, A]

  trait AnyRefRules {
    case class NotNullRule[A](name: String) extends Rule[A]
    case class ValidRule[A](name: String, value: Rules[A]) extends Rule[A]
  }
  trait StringRules {
    case class NotEmptyRule[S, A](name: String, value: String => A) extends Rule[A]
  }

  object rules extends AnyRefRules with StringRules

  object anyRefs {
    def notNull[A](name: String): Validator[A] = Validator { value =>
      if (value == null) NotNull(name).failureNel[A]
      else value.successNel[ValidationFault]
    }

    def valid[A](name: String, ruleSet: Rules[A]): Validator[A] =
      build[A](name + ".", ruleSet)
  }
  object strings {
    def notEmpty(name: String): Validator[String] = Validator { value =>
      if (value.isEmpty) Required(name).failureNel[String]
      else value.successNel[ValidationFault]
    }
  }

  object dsl {
    import rules._

    private[this] def getterToFun[S, A](lens: monocle.Lens[S, A]): S => A =
      s => lens.get(s)

    private[this] def lift[A](value: Rule[A]): Rules[A] = FreeAp.lift[Rule, A](value)

    def notNull[A](name: String): Rules[A] = lift[A](NotNullRule(name))

    def notEmpty(name: String): Rules[String] = lift[String](NotEmptyRule(name, identity))

    def valid[A](name: String, ruleSet: Rules[A]): Rules[A] = lift[A](ValidRule(name, ruleSet))
  }

  def ruleCompiler(prefix: String) = new (Rule ~> ValidatorMonad) {
    import rules._

    def apply[A](rule: Rule[A]): ValidatorMonad[A] = liftM[A](Validator { value => rule match {
      case NotNullRule(name) =>
        if (value == null) NotNull(name).failureNel[A]
        else value.successNel[ValidationFault]
      case NotEmptyRule(name, v) =>
        if (value.asInstanceOf[String].isEmpty) Required(name).failureNel[A]
        else value.successNel[ValidationFault]
      case ValidRule(name, rs) =>
        build[A](name, rs).eval(value)
    }})
  }

  def validatorCompiler = new (ValidatorCo ~> Validated) {
    def apply[A](fa: ValidatorCo[A]): Validated[A] = ???
  }

  def build[A](ruleSet: Rules[A]): Validator[A] =
    build("", ruleSet)

  private def build[A](prefix: String, ruleSet: Rules[A]): Validator[A] = Validator { value =>
    ruleSet.foldMap(ruleCompiler(prefix)).foldMap(validatorCompiler)
  }

  object example {
    import dsl._
    import syntax.applicative._

    case class Example(a: String, b: String)
    val r: Rules[Example] = (notEmpty("a") |@| notEmpty("b"))(Example)

    val validator = build(r)
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
