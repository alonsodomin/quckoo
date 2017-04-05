import sbt._
import sbt.Keys._

import com.typesafe.sbt.SbtAspectj
import com.typesafe.sbt.SbtAspectj.AspectjKeys
import com.typesafe.sbt.web.Import._

import spray.revolver.RevolverPlugin

import org.irundaia.sbt.sass.SbtSassify

import play.twirl.sbt.SbtTwirl

import webscalajs.WebScalaJS

abstract class BaseQuckooServer extends AutoPlugin {
  import RevolverPlugin.autoImport.reStart
  import SbtAspectj.Aspectj
  import AspectjKeys._

  override def requires: Plugins = RevolverPlugin

  lazy val defaultServerSettings: Seq[Def.Setting[_]] = SbtAspectj.aspectjSettings ++ Seq(
    baseDirectory in reStart := baseDirectory.value / "target",
    aspectjVersion in Aspectj := "1.8.10",
    sourceLevel in Aspectj := "1.8",
    javaOptions in reStart ++= (AspectjKeys.weaverOptions in Aspectj).value
  )

  override def projectSettings: Seq[Def.Setting[_]] = defaultServerSettings

}

object QuckooServer extends BaseQuckooServer

object QuckooWebServer extends BaseQuckooServer {
  import WebScalaJS.autoImport.{devCommands, scalaJSPipeline}

  override def requires: Plugins = super.requires && SbtSassify && SbtTwirl

  override lazy val projectSettings: Seq[Def.Setting[_]] = defaultServerSettings ++ Seq(
    compile in Compile := ((compile in Compile) dependsOn scalaJSPipeline).value,
    WebKeys.packagePrefix in Assets := "public/",
    managedClasspath in Runtime += (packageBin in Assets).value,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    devCommands in scalaJSPipeline ++= Seq("test", "testQuick", "testOnly", "docker:publishLocal")
  )

}