import sbt._

object Libraries {

  sealed abstract class Lib {
    val groupId: String
    def prefix: Option[String] = None
    val version: String
    def scalaLib: Boolean = true

    def apply(name: String): ModuleID = {
      val moduleName = prefix.map(_ + s"-$name").getOrElse(name)
      val mod =
        if (scalaLib)
          groupId %% moduleName
        else
          groupId % moduleName
      mod % version withSources() withJavadoc()
    }

  }

  // Akka/Spray Libraries ---------------------------------------

  object Akka extends Lib {
    val groupId = "com.typesafe.akka"
    override val prefix = Some("akka")
    val version = "2.4.0-RC3"
  }

  lazy val akkaLibs: Seq[ModuleID] = Seq(
    Akka("actor"), Akka("remote"), Akka("cluster"), Akka("slf4j"), Akka("cluster-tools"), Akka("testkit") % Test
  )

  object Spray extends Lib {
    val groupId = "io.spray"
    override val prefix = Some("spray")
    val version = "1.3.2"
  }
  lazy val sprayLibs = Seq(Spray("can"), Spray("routing"), Spray("json"), Spray("testkit") % Test)

  // Logging Libraries ---------------------------------------

  object Log4j extends Lib {
    val groupId = "org.apache.logging.log4j"
    override val prefix = Some("log4j")
    val version = "2.3"
    override val scalaLib = false
  }
  lazy val log4jLibs = Seq(Log4j("api"), Log4j("core"), Log4j("slf4j-impl") % Runtime)

  object Slf4s extends Lib {
    val groupId = "org.slf4s"
    override val prefix = Some("slf4s")
    val version = "1.7.12"
  }

  lazy val loggingLibs = log4jLibs ++ Seq(Slf4s("api"),
    "org.slf4j" % "jul-to-slf4j" % Slf4s.version % Runtime withSources() withJavadoc()
  )

  // Testing Libraries ----------------------------------------

  object ScalaTest extends Lib {
    val groupId = "org.scalatest"
    override val prefix = None
    val version = "2.2.4"

    def apply(): ModuleID = apply("scalatest")

  }

  object ScalaMock extends Lib {
    val groupId = "org.scalamock"
    override val prefix = Some("scalamock")
    val version = "3.2.2"
  }

  lazy val testingLibs = Seq(ScalaTest() % Test, ScalaMock("scalatest-support") % Test)

}
