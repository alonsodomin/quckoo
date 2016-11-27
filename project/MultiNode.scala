import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.autoImport._
import de.heikoseeberger.sbtheader.HeaderPlugin
import sbt.Keys._
import sbt._

object MultiNode {

  lazy val settings: Seq[Def.Setting[_]] = SbtMultiJvm.multiJvmSettings ++ Seq(
    libraryDependencies ++= Seq(
      "com.typesafe.akka" %% "akka-multi-node-testkit"  % Dependencies.version.akka.main,
      "org.scoverage"     %% "scalac-scoverage-runtime" % "1.3.0"
    ).map(_ % MultiJvm),
    compile in MultiJvm := ((compile in MultiJvm) triggeredBy (compile in Test)).value,
    parallelExecution in Test := false,
    jvmOptions in MultiJvm := Seq("-Xmx512M"),
    executeTests in Test := {
      val testResults = (executeTests in Test).value
      val multiNodeResults = (executeTests in MultiJvm).value
      val overall = {
        if (testResults.overall.id < multiNodeResults.overall.id)
          multiNodeResults.overall
        else
          testResults.overall
      }
      Tests.Output(overall,
        testResults.events ++ multiNodeResults.events,
        testResults.summaries ++ multiNodeResults.summaries
      )
    }
  ) ++ HeaderPlugin.settingsFor(MultiJvm)

}
