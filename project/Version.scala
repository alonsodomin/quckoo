object Version {
  // Logging -------

  val slogging = "0.6.1"
  val log4j    = "2.13.0"
  val slf4j    = "1.7.30"

  // Testing --------

  val scalaTest  = "3.1.0"
  val scalaCheck = "1.14.3"
  val scalaMock  = "3.6.0"
  val wiremock   = "2.25.1"

  // Akka ----------

  object akka {
    val main = "2.6.3"
    val kryo = "0.9.5"

    val constructr = "0.9.0"

    object http {
      val main = "10.1.11"

      // http extensions
      val json = "1.30.0"
      val sse  = "3.0.0"
    }

    // persistence plugins
    val cassandra = "0.102"
    val inmemory  = "2.5.15.2"
  }

  // Monitoring ----

  object kamon {
    val core       = "1.1.6"
    val akka       = "1.1.4"
    val remote     = "1.0.1"
    val http       = "1.1.3"
    val scala      = "1.1.0"
    val sysmetrics = "1.0.1"
    val prometheus = "1.1.2"
  }

  // ScalaJS -------

  val scalaJsReact   = "1.6.0"
  val scalaJsDom     = "0.9.8"
  val scalaJsJQuery  = "0.9.6"
  val scalaJSScripts = "1.1.4"
  val testState      = "2.3.0"

  // Other utils ---

  val arm         = "2.0"
  val betterfiles = "3.8.0"
  object diode {
    val core  = "1.1.6"
    val react = s"$core.142"
  }
  object cats {
    val main    = "2.1.0"
    val mtl     = "0.7.0"
    val effect  = "2.0.0"
    val kittens = "2.0.0"
    val testkit = "1.0.0-RC1"
  }
  val circe        = "0.12.3"
  val cron4s       = "0.6.0"
  val enumeratum   = "1.5.22"
  val ivy          = "2.5.0"
  val monix        = "3.0.0"
  val monocle      = "2.0.1"
  val pureconfig   = "0.12.2"
  val refined      = "0.9.12"
  val scalaCss     = "0.6.0"
  val scalaTime    = "2.0.0-RC3"
  val scalaLocales = "0.5.2-cldr31"
  val scopt        = "3.7.1"
  val scoverage    = "1.4.1"
  val sttp         = "1.7.2"
  val xml          = "1.2.0"

  // JavaScript Libraries

  val animate          = "3.2.2"
  val jquery           = "2.2.4"
  val bootstrap        = "3.3.7"
  val bootstrapNotifiy = "3.1.3"
  val reactJs          = "16.7.0"
  val sparkMD5         = "3.0.0"
  val codemirror       = "5.50.0"
}
