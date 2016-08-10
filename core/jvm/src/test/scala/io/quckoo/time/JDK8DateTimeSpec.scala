package io.quckoo.time

import java.time.ZonedDateTime

import org.scalacheck._

//import org.scalatest.{FlatSpec, Matchers}

/**
  * Created by alonsodomin on 10/08/2016.
  */
class JDK8DateTimeSpec extends Properties("JDK8DateTime") {
  import JDK8TimeGenerators._
  import Arbitrary.arbitrary
  import Prop._

  val dateTimes = for {
    zdt <- arbitrary[ZonedDateTime]
  } yield new JDK8DateTime(zdt)
  implicit lazy val arbitraryDateTime = Arbitrary(dateTimes)

  property("isEqual") = forAll { (dt: JDK8DateTime) =>
    dt.isEqual(dt)
  }

  property("isAfter") = forAll { (left: JDK8DateTime, right: JDK8DateTime) =>
    left.underlying.isAfter(right.underlying) ==> left.isAfter(right)
  }

  property("isBefore") = forAll { (left: JDK8DateTime, right: JDK8DateTime) =>
    left.underlying.isBefore(right.underlying) ==> left.isBefore(right)
  }

}
