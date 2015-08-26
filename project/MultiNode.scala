import com.typesafe.sbt.SbtMultiJvm
import com.typesafe.sbt.SbtMultiJvm.MultiJvmKeys.MultiJvm
import sbt.Keys._
import sbt._

object MultiNode {
  import Libraries._

  lazy val settings: Seq[Def.Setting[_]] = SbtMultiJvm.multiJvmSettings ++ Seq(
    libraryDependencies ++= Seq(
      Akka("remote"), Akka("multi-node-testkit")
    ),
    libraryDependencies in MultiJvm <<= (libraryDependencies in Test),
    compile in MultiJvm <<= (compile in MultiJvm) triggeredBy (compile in Test),
    parallelExecution in Test := false,
    executeTests in Test <<= (executeTests in Test, executeTests in MultiJvm) map {
      case (testResults, multiNodeResults) =>
        val overall = if (testResults.overall.id < multiNodeResults.overall.id)
          multiNodeResults.overall
        else testResults.overall
        Tests.Output(overall,
          testResults.events ++ multiNodeResults.events,
          testResults.summaries ++ multiNodeResults.summaries
        )
    }
  )

  def apply(project: Project): Project = project.
    settings(this.settings: _*).
    configs(MultiJvm)
  
}