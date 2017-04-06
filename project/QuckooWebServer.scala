import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtAspectj
import com.typesafe.sbt.SbtAspectj.AspectjKeys
import com.typesafe.sbt.web.Import._

import spray.revolver.RevolverPlugin

import org.irundaia.sbt.sass.SbtSassify

import play.twirl.sbt.SbtTwirl

import webscalajs.WebScalaJS

trait QuckooAppKeys {
  val aspectjWeaver = taskKey[File]("Aspectj Weaver jar file")
  val sigarLoader = taskKey[File]("Sigar loader jar file")
  val sigarLoaderOptions = taskKey[Seq[String]]("JVM options for the Sigar loader")
}
object QuckooAppKeys extends QuckooAppKeys

object QuckooApp extends AutoPlugin {
  import RevolverPlugin.autoImport.reStart
  import SbtAspectj.Aspectj
  import AspectjKeys._
  import QuckooAppKeys._

  override def requires: Plugins = RevolverPlugin

  lazy val defaultServerSettings: Seq[Def.Setting[_]] = SbtAspectj.aspectjSettings ++ Seq(
    aspectjWeaver := findAspectjWeaver(update.value),
    sigarLoader := findSigarLoader(update.value),
    sigarLoaderOptions := Seq(s"-javaagent:${sigarLoader.value.getAbsolutePath}"),
    baseDirectory in reStart := baseDirectory.value / "target",
    aspectjVersion in Aspectj := "1.8.10",
    sourceLevel in Aspectj := "1.8",
    javaOptions in reStart ++= (AspectjKeys.weaverOptions in Aspectj).value ++ sigarLoaderOptions.value
  )

  override def projectSettings: Seq[Def.Setting[_]] = defaultServerSettings

  private[this] val aspectjWeaverFilter: DependencyFilter =
    configurationFilter(Aspectj.name) && artifactFilter(name = "aspectjweaver", `type` = "jar")

  private[this] def findAspectjWeaver(report: UpdateReport) =
    report.matching(aspectjWeaverFilter).head

  private[this] def findSigarLoader(report: UpdateReport) =
    report.matching(artifactFilter(name = "sigar-loader", `type` = "jar")).head

}

object QuckooWebServer extends AutoPlugin {
  import WebScalaJS.autoImport.{devCommands, scalaJSPipeline}

  override def requires: Plugins = QuckooApp && SbtSassify && SbtTwirl

  override lazy val projectSettings: Seq[Def.Setting[_]] = Seq(
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    devCommands in scalaJSPipeline ++= Seq("test", "testQuick", "testOnly", "docker:publishLocal")
  )

}